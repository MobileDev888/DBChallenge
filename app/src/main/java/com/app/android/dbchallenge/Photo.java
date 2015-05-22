package com.app.android.dbchallenge;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class represents the Photo class
 */
public class Photo implements Parcelable{
    private static final String JSON_FILE_NAME = "fileName";
    private static final String JSON_PHOTO_NOTE = "photoNote";
    private String mFileName;
    private double mLatitude;
    private double mLongitude;
    private boolean mMappable;
    private String mCity;
    private String mUrl;
    private String mPhotoNote;

    public Photo(){}

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public boolean isMappable() {
        return mMappable;
    }

    public void setMappable(boolean mappable) {
        this.mMappable = mappable;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        this.mCity = city;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public void setPhotoNote(String photoNote) {
        this.mPhotoNote = photoNote;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mFileName);
        parcel.writeDouble(mLatitude);
        parcel.writeDouble(mLongitude);
        parcel.writeBooleanArray(new boolean[]{mMappable});
        parcel.writeString(mCity);
        parcel.writeString(mUrl);
        parcel.writeString(mPhotoNote);
    }

    protected Photo(Parcel in) {
        mFileName = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();

        boolean[] myBooleanArr = new boolean[1];
        in.readBooleanArray(myBooleanArr);
        mMappable = myBooleanArr[0];
        mCity = in.readString();
        mUrl = in.readString();
        mPhotoNote = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Photo createFromParcel(Parcel in) {
            return new Photo(in);
        }

        @Override
        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

}
