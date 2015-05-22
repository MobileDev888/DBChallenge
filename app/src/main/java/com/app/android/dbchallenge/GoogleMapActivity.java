package com.app.android.dbchallenge;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

/**
 * Created by Android1 on 5/21/2015.
 */
public class GoogleMapActivity extends Activity {
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_google_map);
        ArrayList<Photo> photoList = getIntent().getParcelableArrayListExtra(MainActivity.PHOTO_LIST);
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        LatLng latLing = new LatLng(0, 0);

        for(int i = 0; i < photoList.size(); i++){

            if(photoList.get(i).isMappable()) { //if the photo has LatLing info
                latLing = new LatLng(photoList.get(i).getLatitude(), photoList.get(i).getLongitude());

                //Add a marker on the map
                map.addMarker(new MarkerOptions()
                        .position(latLing)
                        .title(photoList.get(i).getFileName()));
            }
        }

        //Zoom into the last marker
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLing, 15));
    }
}