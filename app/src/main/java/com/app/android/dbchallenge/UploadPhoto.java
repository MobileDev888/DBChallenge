/*
 * Copyright (c) 2011 Dropbox, Inc.
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * This AsyncTask class uploads a photo to the DropBox in a worker thread
 */
public class UploadPhoto extends AsyncTask<Void, Long, Boolean> {

    private DropboxAPI<?> mDropBoxApi;
    private String mDirectory;
    private File mFile;

    private long mFileLength;
    private UploadRequest mRequest;
    private Context mContext;
    private final ProgressDialog mProgressDialog;

    private String mErrorMsg;
    private final static int UPDATE_INTERVAL = 1000; //Update the progress every 1 second

    public UploadPhoto(Context context, DropboxAPI<?> api, String photoDirectory, File file) {
        mContext = context;
        mFileLength = file.length();
        mDropBoxApi = api;
        mDirectory = photoDirectory;
        mFile = file;

        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMax(100);
        mProgressDialog.setMessage(mContext.getString(com.app.android.dbchallenge.R.string.uploading) + mFile.getName());
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setButton(ProgressDialog.BUTTON_POSITIVE,
                mContext.getString(com.app.android.dbchallenge.R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRequest.abort();
            }
        });
        mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            FileInputStream fileInputStream = new FileInputStream(mFile);
            String filePath = mDirectory + mFile.getName();

            mRequest = mDropBoxApi.putFileOverwriteRequest(filePath, fileInputStream, mFile.length(),
                    new ProgressListener() {
                @Override
                public long progressInterval() {
                    return UPDATE_INTERVAL;
                }

                @Override
                public void onProgress(long bytes, long total) {
                    publishProgress(bytes);
                }
            });

            if (mRequest != null) {
                mRequest.upload();
                return true;
            }

        } catch(DropboxUnlinkedException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.not_authenticated_msg);
        } catch(DropboxFileSizeException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.file_too_big_msg);
        } catch(DropboxPartialFileException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.upload_canceled_msg);
        } catch(DropboxServerException e) {
            mErrorMsg = e.body.userError; // Translate DropboxServerException to readable language
            if (mErrorMsg == null)
                mErrorMsg = e.body.error;
        } catch(DropboxIOException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.network_error_msg);
        } catch(DropboxParseException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.dropbox_error_msg);
        } catch(DropboxException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.dropbox_error_msg);
        } catch(FileNotFoundException e){

        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int)(100.0*(double)progress[0]/mFileLength);
        mProgressDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mProgressDialog.dismiss();
        if (result) {
            showToast(mContext.getString(com.app.android.dbchallenge.R.string.photo_uploaded_msg));
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }
}
