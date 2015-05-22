package com.app.android.dbchallenge;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class ApplicationInstance extends Application {
    private static ApplicationInstance pApplication;
    private Context pAppContext;
    private ImageLoader imageLoader;
    private RequestQueue queue;

    @Override
    public void onCreate() {
        super.onCreate();

        pApplication = this;
        pAppContext = getApplicationContext();
        queue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(queue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);
                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
        });
    }

    public static ApplicationInstance getApplication() {
        return pApplication;
    }

    private RequestQueue getRequestQueue() {
        return queue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
