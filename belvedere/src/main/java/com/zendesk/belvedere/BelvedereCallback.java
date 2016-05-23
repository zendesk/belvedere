package com.zendesk.belvedere;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

/**
 * Simple Callback used to deliver results results asynchronously.
 *
 * <p>
 *     As always, keep a strong reference to the callback
 *     in your {@link Activity}/{@link Fragment}. Don't forget
 *     to call {@link #cancel()} and revoke the reference to this callback
 *     when your {@link Activity}/{@link Fragment} gets destroyed.
 * </p>
 *
 * @param <E> The result type.
 */
public abstract class BelvedereCallback<E> {

    private boolean canceled = false;

    public BelvedereCallback(){
        // Intentionally empty
    }

    /**
     * Cancel this callback. {@link #success(Object)} won't be called.
     */
    public void cancel(){
        this.canceled = true;
    }

    void internalSuccess(final E result){
        if(!canceled){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    BelvedereCallback.this.success(result);
                }
            });
        }
    }

    /**
     * Method used to deliver results.
     * <p>
     *     Will be invoked on the main thread.
     * </p>
     *
     * @param result The result.
     */
    public abstract void success(E result);
}
