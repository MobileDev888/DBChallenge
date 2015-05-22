package com.app.android.dbchallenge;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * This class provides methods to control audio files (Play, Record, Stop)
 */
public class AudioFileManager {
    private static final String LOG_TAG = "AudioRecordTest";
    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;
    private static Context mContext;

    public AudioFileManager(Context context){
        mContext = context;
    }

    public void onRecord(boolean start, String fileName) {
        if (start) {
            startRecording(fileName);
        } else {
            stopRecording();
        }
    }

    public void play(String fileName) {
        stop();
        mPlayer = new MediaPlayer();
        String filePath = mContext.getFilesDir() + fileName + ".3gp";
        File file = new File(filePath);

        if(file.exists()) {

            try {
                mPlayer.setDataSource(filePath);
                mPlayer.prepare();
                mPlayer.start();

                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        stop();
                    }
                });
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }
        } else { //If the file doesn't exist, show an alert dialog
            Resources res = mContext.getResources();
            String title = res.getString(R.string.no_file_dialog_title);
            String message = res.getString(R.string.no_file_dialog_msg);
            showAlertDialog(title, message, mContext);
        }
    }

    public static void showAlertDialog(String title, String alertMessage, Context context) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.ok, null)
                .show();
    }


    public void stop() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void startRecording(String fileName) {
        String filePath = mContext.getFilesDir() + fileName + ".3gp";
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(filePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopRecording() {
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        } catch(Exception e){
            Log.e(LOG_TAG, "stopRecording() failed");
        }
    }

    public void cleanUpMediaPlayer(){
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        if(mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

    }
}
