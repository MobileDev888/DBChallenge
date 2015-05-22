package com.app.android.dbchallenge;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import java.util.ArrayList;

/**
 * Created by Android1 on 5/21/2015.
 */
public class ShowPhotoActivity extends ActionBarActivity {
    private static final String PHOTO_NOTE_PREFERENCES = "photo_note_preferences";
    private ImageView mImage;
    private int mPosition;
    CallbackManager callbackManager;
    ShareDialog shareDialog;
    private ArrayList<Photo> mPhotoList;
    private String mCity;
    private Button mRecordButton;
    private Button mStopButton;
    private Button mPlayButton;
    boolean mStartRecording = true;
    AudioFileManager mAudioManager;
    private EditText mPhotoNoteEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);
        mImage = (ImageView)this.findViewById(R.id.imageView);
        mPosition = getIntent().getIntExtra(MainActivity.POSITION, 0);
        downloadPhoto();

        mPhotoList = getIntent().getParcelableArrayListExtra(MainActivity.PHOTO_LIST);
        mCity = mPhotoList.get(mPosition).getCity();

        Button postFacebookBtn = (Button)findViewById(R.id.post_facebook_btn);
        postFacebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postToFacebook();
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                if (result.getPostId() != null) {
                    String title = getString(R.string.facebook_success);
                    showResult(title, "");
                }
            }

            private void showResult(String title, String alertMessage) {
                new AlertDialog.Builder(ShowPhotoActivity.this)
                        .setTitle(title)
                        .setMessage(alertMessage)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {
                String title = getString(R.string.error);
                showResult(title, e.getMessage());
            }
        });

        mAudioManager = new AudioFileManager(this);
        mRecordButton = (Button)findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                mAudioManager.onRecord(mStartRecording, String.valueOf(mPhotoList.get(mPosition).getFileName()));
                if (mStartRecording) {
                    mPlayButton.setEnabled(false);
                    mStopButton.setEnabled(false);
                    mRecordButton.setText(R.string.stop_record_button_label);
                } else {
                    mPlayButton.setEnabled(true);
                    mStopButton.setEnabled(true);
                    mRecordButton.setText(R.string.start_record_button_label);
                }
                mStartRecording = !mStartRecording;
            }
        });

        mStopButton = (Button)findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAudioManager.stop();
            }
        });

        mPlayButton = (Button)findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAudioManager.play(mPhotoList.get(mPosition).getFileName());
            }
        });

        mPhotoNoteEditText = (EditText)findViewById(R.id.photo_note_editText);
        mPhotoNoteEditText.setText(getPhotoNote(mPhotoList.get(mPosition).getFileName()));
        mPhotoNoteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mPhotoList.get(mPosition).setPhotoNote(charSequence.toString());
                savePhotoNote(mPhotoList.get(mPosition).getFileName(), charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    private void savePhotoNote(String key, String value){
        SharedPreferences sharedPref = getSharedPreferences(PHOTO_NOTE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getPhotoNote(String key){
        SharedPreferences preferences = getSharedPreferences(PHOTO_NOTE_PREFERENCES, 0);
        return preferences.getString(key, "");
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    /**
     * This method executes DisplayPhoto AsyncTask which downloads a photo
     */
    protected void downloadPhoto(){
        String photoDirectory = MainActivity.PHOTO_DIR;
        DropboxAPI<AndroidAuthSession> dropboxAPI = MainActivity.getDropboxAPI();
        DisplayPhoto download = new DisplayPhoto(ShowPhotoActivity.this, dropboxAPI, photoDirectory, mImage, mPosition);
        download.execute();
    }

    /**
     * This method builds the content of a Facebook post
     */
    public void postToFacebook(){
        String contentTitle = "";

        if(mCity != null){
            contentTitle = getResources().getString(R.string.photo_taken_in) + " " + mCity;
        }

        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentTitle(contentTitle)
                    .setContentUrl(Uri.parse(mPhotoList.get(mPosition).getUrl()))
                    .build();

            shareDialog.show(linkContent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop(){
        super.onStop();
        mAudioManager.cleanUpMediaPlayer();
    }

}
