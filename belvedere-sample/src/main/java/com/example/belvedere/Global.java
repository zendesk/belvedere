package com.example.belvedere;

import android.app.Application;

import zendesk.belvedere.Belvedere;


public class Global extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader();
    }

    private void initImageLoader(){
        Belvedere belvedere = new Belvedere.Builder(this)
                .debug(true)
                .build();
        Belvedere.setSingletonInstance(belvedere);
    }
}
