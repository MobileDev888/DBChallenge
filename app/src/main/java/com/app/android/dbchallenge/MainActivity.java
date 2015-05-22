/*
 * Copyright (c) 2010-11 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.app.android.dbchallenge;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends Activity {
    private static final String ACCOUNT_PREFERENCES = "account_preferences";
    private static final String APP_SECRET = "ACCESS_SECRET";
    private static final String JPG = ".jpg";
    public static final String POSITION = "com.app.android.dbchallenge.position";
    public static final String PHOTO_LIST = "com.app.android.dbchallenge.photo_list";
    private static DropboxAPI<AndroidAuthSession> mDropBoxApi;

    private boolean mLoggedIn;
    private boolean mFetched = false;
    // UI widgets
    private Button mLoginBtn;
    private Button mTakePhotoBtn;
    private Button mGoogleMapBtn;
    private ImageView mImage;
    public static final String PHOTO_DIR = "/Photos/";
    private PhotoRecyclerAdapter mAdapter = null;
    private static final int NEW_PICTURE = 1;
    private String mCameraFileName;
    private AndroidAuthSession mSession;
    private PhotoHandler mHandler;
    private ArrayList<Photo> mPhotoList = null;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Save these data; so that, the list doesn't need to be loaded again
        if (savedInstanceState != null) {
            mFetched = savedInstanceState.getBoolean("mFetched");
            mPhotoList = null;
            mPhotoList = savedInstanceState.getParcelableArrayList("mPhotoList");
        }
        setContentView(R.layout.activity_main);
        mHandler = new PhotoHandler();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(this.getString(R.string.loading));

        //Create an AndroidAuthSession to use the DropBox API
        mSession = createSession();
        mDropBoxApi = new DropboxAPI<AndroidAuthSession>(mSession);

        mLoginBtn = (Button)findViewById(com.app.android.dbchallenge.R.id.login_button);
        mLoginBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mLoggedIn)
                    logOut();
                else
                    logIn();
            }
        });

        // This is where a photo is displayed
        mImage = (ImageView)findViewById(R.id.imageView);

        // This is the button to take a photo
        mTakePhotoBtn = (Button)findViewById(com.app.android.dbchallenge.R.id.take_photo_btn);
        mTakePhotoBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                //Create an intent to start camera and the picture taken is stored in uri
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                String fileName = String.valueOf(Calendar.getInstance().getTimeInMillis()) + JPG;
                String outFilePath = new File(Environment.getExternalStorageDirectory(), fileName).getPath();
                File outFile = new File(outFilePath);
                mCameraFileName = outFile.toString();
                Uri uri = Uri.fromFile(outFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                try {
                    startActivityForResult(intent, NEW_PICTURE);
                } catch (ActivityNotFoundException e) {
                    showToast(getResources().getString(com.app.android.dbchallenge.R.string.no_camera_msg));
                }
            }
        });

        // This is the button to open Google map
        mGoogleMapBtn = (Button)findViewById(R.id.google_map_btn);
        mGoogleMapBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), GoogleMapActivity.class);
                intent.putParcelableArrayListExtra(PHOTO_LIST, mPhotoList);
                startActivity(intent);
            }
        });

        setLoggedIn(mDropBoxApi.getSession().isLinked());
    }

    public static DropboxAPI<AndroidAuthSession> getDropboxAPI() {
        return mDropBoxApi;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("mFetched", mFetched);
        outState.putParcelableArrayList("mPhotoList", mPhotoList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mDropBoxApi.getSession();

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication(); //require to complete the authentication
                storeOAuth2(session);
                setLoggedIn(true);

                if(!mFetched) { //if photolist not loaded
                    loadPhotoList();
                }
            } catch (IllegalStateException e) {
                showToast(getResources().getString(com.app.android.dbchallenge.R.string.logout_issue_msg) + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                File file = new File(mCameraFileName);
                UploadPhoto upload = new UploadPhoto(this, mDropBoxApi, PHOTO_DIR, file);
                upload.execute(); //upload the photo if there is no error
            } else {
                showToast(getResources().getString(com.app.android.dbchallenge.R.string.post_camera_warning_msg) + resultCode);
            }
        }
    }

    private AndroidAuthSession createSession() {
        AppKeyPair appKeyPair = new AppKeyPair(DeveloperKey.DROPBOX_APP_KEY, DeveloperKey.DROPBOX_APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadOAuth2(session);
        return session;
    }

    /**
     * Keeps the app secret in a local store; so that, users don't need to authenticate
     * each time
     */
    private void loadOAuth2(AndroidAuthSession session) {
        SharedPreferences preferences = getSharedPreferences(ACCOUNT_PREFERENCES, 0);
        String secret = preferences.getString(APP_SECRET, null);

        if (secret == null || secret.length() == 0) return;
        session.setOAuth2AccessToken(secret); //OAuth2 support
    }

    /**
     * Sets the UI based on the loggedIn state
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            mLoginBtn.setText(getString(R.string.logout_btn_label));

            if(mFetched) {

                if(mAdapter == null)
                    setUpPhotoList(true);
                else
                    mAdapter.notifyDataSetChanged();

                mTakePhotoBtn.setEnabled(true);
                mGoogleMapBtn.setEnabled(true);
            } else {
                loadPhotoList();
            }
        } else {
            mLoginBtn.setText(getString(com.app.android.dbchallenge.R.string.login_btn_label));
            setUpPhotoList(false);
            mTakePhotoBtn.setEnabled(false);
            mGoogleMapBtn.setEnabled(false);
        }
    }

    private void loadPhotoList(){
        SharedPreferences preferences = getSharedPreferences(ACCOUNT_PREFERENCES, 0);
        String secret = preferences.getString(APP_SECRET, null);
        if (secret == null || secret.length() == 0) return;
        mFetched = true;
        mPhotoList = null;
        mProgressDialog.show();
        PhotoListUtility.loadPhotoList(secret, mHandler, this);
    }

    /**
     * This method performs log out from DropBox which removes the credentials,
     * clears stored key and secret and set the correct UI loggedIn state
     */
    private void logOut() {
        mDropBoxApi.getSession().unlink(); //Remove the credentials

        //Clear the stored and secret
        SharedPreferences preferences = getSharedPreferences(ACCOUNT_PREFERENCES, 0);
        Editor edit = preferences.edit();
        edit.clear();
        edit.commit();
        setLoggedIn(false);
        mFetched = false;
        mPhotoList = null;
        mAdapter = null;
    }

    /**
     * This method performs remote authentication to DropBox
     */
    private void logIn(){
        mDropBoxApi.getSession().startOAuth2Authentication(MainActivity.this);
    }

    /**
     * Stores the app secret in shared preferences
     */
    private void storeOAuth2(AndroidAuthSession session) {
        String oauth2AccessToken = session.getOAuth2AccessToken();

        if (oauth2AccessToken != null) {
            SharedPreferences preferences = getSharedPreferences(ACCOUNT_PREFERENCES, 0);
            Editor edit = preferences.edit();
            edit.putString(APP_SECRET, oauth2AccessToken);
            edit.commit();
            return;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public class PhotoHandler extends Handler {

        @Override
        public void handleMessage(Message message){
            if(isFinishing())return;

            if(message.what == 1){ //if photolist has been loaded
                mProgressDialog.dismiss();
                mPhotoList = PhotoListUtility.getPhotoList();
                setLoggedIn(true);
            } else if(message.what == 2){
                showToast(getResources().getString(R.string.loading_photo_list_error));
            }
        }
    }

    /**
     * This method sets up the photo list RecyclerView based on isEnabled
     */
    private void setUpPhotoList(boolean isEnabled){
        LinearLayout parentView = (LinearLayout)findViewById(R.id.photo_list_display);

        if(isEnabled) {
            mAdapter = new PhotoRecyclerAdapter(this, R.layout.photo_info, mPhotoList);
            android.support.v7.widget.RecyclerView rv = new android.support.v7.widget.RecyclerView(this);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rv.setAdapter(mAdapter);
            rv.setItemAnimator(new DefaultItemAnimator());
            parentView.addView(rv);
        } else {
            if(parentView.getChildCount() > 0)
                parentView.removeAllViews();
        }
    }

    /**
     * This method starts the ShoPhotoActivity
     */
    public void showPhoto(int pos){
        Intent intent = new Intent(this, ShowPhotoActivity.class);
        intent.putExtra(POSITION,pos);
        intent.putParcelableArrayListExtra(PHOTO_LIST, mPhotoList);
        startActivity(intent);
    }
}
