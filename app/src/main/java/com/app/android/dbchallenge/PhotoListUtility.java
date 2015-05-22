package com.app.android.dbchallenge;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Message;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.app.android.dbchallenge.MainActivity.PhotoHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * Created by Android1 on 5/21/2015.
 */
public class PhotoListUtility {
    private static final String TAG = "PhotoListUtility";
    private static ArrayList<Photo> mPhotoList;

    //These image formats are supported in DropBox
    private final static String[] supportedFormats = {".jpg", ".jpeg", ".png", ".tiff", ".tif",
            ".gif", ".bmp", ".ai", ".psd", ".svg"};

    /**
     * This method loads the photolist based on the returned JSON object from a DropBox API endpoint
     */
    public static void loadPhotoList(String secret, PhotoHandler handler, Activity activity){
        final String oauth2AccessToken = secret;
        final PhotoHandler pHandler = handler;
        final Activity pActivity = activity;
        mPhotoList = new ArrayList<Photo>();

        JsonObjectRequest myRequest = new JsonObjectRequest(
                Request.Method.GET,
                "https://api.dropbox.com/1/metadata/auto/Photos?include_media_info=true", //API endpoint

                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (pActivity.isFinishing()) return;

                        try {//The code in try block gets data from JSON and loads the photo list accordingly
                            JSONObject j = response;
                            JSONArray jarr, locationJarr;
                            jarr = j.getJSONArray("contents");
                            double latitude;
                            double longitude;

                            for(int index = 0; index < jarr.length(); index++){
                                j = (JSONObject)jarr.get(index);
                                String path = j.getString("path");
                                path = path.substring(8, path.length());

                                if(isSupported(path)) {
                                    j = j.getJSONObject("photo_info");
                                    Photo photo = new Photo();

                                    try {
                                        if (j.getJSONArray("lat_long") != null) {
                                            locationJarr = j.getJSONArray("lat_long");
                                            latitude = locationJarr.getDouble(0);
                                            longitude = locationJarr.getDouble(1);
                                            photo.setFileName(path);
                                            photo.setLatitude(latitude);
                                            photo.setLongitude(longitude);
                                            photo.setMappable(true);
                                        }
                                    }catch(JSONException e){
                                        Log.e(TAG, e.getMessage());
                                        photo.setFileName(path);
                                        photo.setMappable(false);
                                    }
                                    mPhotoList.add(photo);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }

                        try {//This block of code gets the city names and assigns it to the photo list elements
                            for (int i = 0; i < mPhotoList.size(); i++) {

                                if (mPhotoList.get(i).isMappable()) {
                                    Geocoder gcd = new Geocoder(pActivity.getApplicationContext(), Locale.getDefault());
                                    List<Address> addresses = gcd.getFromLocation(mPhotoList.get(i).getLatitude(),
                                            mPhotoList.get(i).getLongitude(), 1);
                                    if (addresses.size() > 0)
                                        mPhotoList.get(i).setCity(addresses.get(0).getLocality());
                                }
                            }
                        }catch(IOException e){
                            Log.e(TAG, e.getMessage());
                        }

                        //loads photo URL to each element of the photo list
                        for(int i = 0; i < mPhotoList.size(); i++) {
                            PhotoListUtility.loadPhotoURL(oauth2AccessToken, pHandler, pActivity, i);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Message message = new Message();
                        message.what = 2;
                        pHandler.sendMessage(message);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + oauth2AccessToken); //pass the accessToken thru header
                return headers;
            }
        };
        ApplicationInstance.getApplication().addToRequestQueue(myRequest); //put the request in the queue
    }

    /**
     * This method loads the photo URL based on the returned JSON object from a DropBox API endpoint
     */
    private static void loadPhotoURL(String secret, PhotoHandler handler, Activity activity, int pos){
        final String oauth2AccessToken = secret;
        final PhotoHandler pHandler = handler;
        final Activity pActivity = activity;
        final int position = pos;
        final String pFileName = mPhotoList.get(position).getFileName();

        JsonObjectRequest myRequest = new JsonObjectRequest(
                Request.Method.GET,
                "https://api.dropbox.com/1/media/auto/Photos/" + pFileName, //The API endpoint to get URL

                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (pActivity.isFinishing()) return;

                        try {
                            JSONObject j = response;
                            String url = j.getString("url");
                            mPhotoList.get(position).setUrl(url);
                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                        }

                        //After finishing the last URL, tell the MainActivity that it is done
                        if(position == mPhotoList.size() - 1) {
                            Message message = new Message();
                            message.what = 1;
                            pHandler.sendMessage(message);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Message message = new Message();
                        message.what = 2;
                        pHandler.sendMessage(message);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + oauth2AccessToken);
                return headers;
            }
        };
        ApplicationInstance.getApplication().addToRequestQueue(myRequest);
    }

    public static ArrayList<Photo> getPhotoList(){
        return mPhotoList;
    }

    /**
     * This method checks if the file format in the input parameter is supported in DropBox
     */
    private static boolean isSupported(String file){

        for(int i = 0; i < supportedFormats.length; i++){
            if(file.endsWith(supportedFormats[i]))
                return true;
        }
        return false;
    }
}
