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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.ThumbFormat;
import com.dropbox.client2.DropboxAPI.ThumbSize;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The AsyncTask downloads the selected photo in the background and shows it when the download
 * is finished.
 */

public class DisplayPhoto extends AsyncTask<Void, Long, Boolean> {
    private Context mContext;
    private final ProgressDialog mProgressDialog;
    private DropboxAPI<?> mDropboxApi;
    private String mFilePath;
    private ImageView mImageView;
    private Drawable mDrawable;
    private FileOutputStream mFileOutputStream;
    private boolean mCanceled;
    private Long mFileLength;
    private String mErrorMsg;
    private final static String IMAGE_FILE_NAME = "temp.png"; //temporary file name on device
    private int mPosition;
    private static String mCachePath;

    public DisplayPhoto(Context context, DropboxAPI<?> api, String dropboxPath, ImageView view, int pos) {
        mContext = context;
        mDropboxApi = api;
        mFilePath = dropboxPath;
        mImageView = view;
        mPosition = pos;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(context.getString(R.string.downloading));
        mProgressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, context.getString(R.string.cancel), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCanceled = true;
                mErrorMsg = mContext.getString(R.string.cancel);

                // Cancel the getThumbnail task
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        });

        mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            if (mCanceled) return false;

            // Get the metadata
            Entry entry = mDropboxApi.metadata(mFilePath, 1000, null, true, null);

            // It's not a directory or the content is null
            if (!entry.isDir || entry.contents == null) {
                mErrorMsg = mContext.getString(R.string.empty_directory);
                return false;
            }

            // Add thumbnails to the arraylist for later use
            ArrayList<Entry> thumbList = new ArrayList<Entry>();
            for (Entry contentEntry: entry.contents) {
                if (contentEntry.thumbExists) {
                    thumbList.add(contentEntry);
                }
            }

            if (mCanceled) return false; //Check again because the code above might take a while

            if (thumbList.size() == 0) {
                mErrorMsg = mContext.getString(R.string.no_photo_directory);
                return false;
            }

            Entry ent = thumbList.get(mPosition);
            String path = ent.path;
            mFileLength = ent.bytes;

            mCachePath = mContext.getCacheDir().getAbsolutePath() + "/" + IMAGE_FILE_NAME;
            try {
                mFileOutputStream = new FileOutputStream(mCachePath);
            } catch (FileNotFoundException e) {
                mErrorMsg = mContext.getString(R.string.create_local_file_problem);
                return false;
            }

            // Download thumbnails in predefined size
            mDropboxApi.getThumbnail(path, mFileOutputStream, ThumbSize.BESTFIT_960x640,
                    ThumbFormat.JPEG, null);

            if (mCanceled) return false; //The code above takes some time and we give it another chance to cancel the task

            mDrawable = Drawable.createFromPath(mCachePath);
            return true;

        } catch (DropboxUnlinkedException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.not_authenticated_msg);
        } catch (DropboxPartialFileException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.download_canceled_msg);
        } catch (DropboxServerException e) {
            mErrorMsg = e.body.userError; // Translate DropboxServerException to readable language
            if (mErrorMsg == null)
                mErrorMsg = e.body.error;
        } catch (DropboxIOException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.network_error_msg);
        } catch (DropboxParseException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.dropbox_error_msg);
        } catch (DropboxException e) {
            mErrorMsg = mContext.getString(com.app.android.dbchallenge.R.string.dropbox_error_msg);
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
            mImageView.setImageDrawable(mDrawable); //Set the photo
        } else {
            showToast(mErrorMsg); //Error occurred, show it
        }
    }

    private void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }


}
