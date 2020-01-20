package com.by5388.pdf.reader;

import android.app.Application;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

/**
 * @author Administrator  on 2020/1/19.
 */
public class ReaderApp extends Application implements Executor {

    private static ReaderApp sInstance;
    private Executor mExecutors;

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutors = Executors.newFixedThreadPool(2);
        sInstance = this;
    }


    @Override
    public void execute(@NonNull Runnable command) {
        mExecutors.execute(command);
    }

    public static ReaderApp getInstance() {
        return sInstance;
    }
}
