package com.example.belvedere;

import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.zendesk.belvedere.Belvedere;

public class Global extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader();
    }

    private void initImageLoader(){
        DisplayImageOptions build = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        ImageLoaderConfiguration builder = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(build)
                .build();

        ImageLoader.getInstance().init(builder);

        Belvedere belvedere = new Belvedere.Builder(this)
                .debug(true)
                .build();
        Belvedere.setSingletonInstance(belvedere);
    }
}
