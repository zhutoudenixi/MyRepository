package com.gl_share.checkface;

import android.app.Application;

import com.lidroid.xutils.HttpUtils;

/**
 * Created by Gl on 2016/1/21.
 */
public class MyApplication extends Application {

    public static HttpUtils httpUtils;

    @Override
    public void onCreate() {
        super.onCreate();

        httpUtils = new HttpUtils();
    }
}
