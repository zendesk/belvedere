package com.zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Model class that is returned by {@link Belvedere#getBelvedereIntents()}.
 * <p>
 *      Holds an {@link Intent}, an expected requestId and a {@link BelvedereSource}.
 * </p>
 * <p>
 *      It's intended to use this object in e.g. a dialog to open an installed gallery
 *      or camera app. For convenience use {@link #open(Activity)} or {@link #open(Fragment)} to
 *      fire the {@link Intent}.
 * </p>
 * <p>
 *      If you don't want to use one of the provided open methods, make sure to respect the provided
 *      {@link #getRequestCode()} when calling {@link Activity#startActivityForResult(Intent, int)}.
 *      {@link Belvedere#getFilesFromActivityOnResult(int, int, Intent, BelvedereCallback)} relies on
 *      that code.
 * </p>
 */
public class BelvedereIntent implements Parcelable {

    private final int requestCode;
    private final Intent intent;
    private final String permission;

    public BelvedereIntent(@NonNull Intent intent, int requestCode, @Nullable String permission){
        this.intent = intent;
        this.requestCode = requestCode;
        this.permission = permission;
    }

    /**
     * Get the {@link Intent}.
     *
     * @return The {@link Intent}
     */
    @NonNull
    public Intent getIntent() {
        return intent;
    }

    /**
     * Get the request code.
     *
     * @return The request code.
     */
    public int getRequestCode() {
        return requestCode;
    }

    /**
     * Return the name of permission, if it's necessary
     * to ask for it during run-time.
     *
     * <p>
     *     The returned {@link String} one of the constants defined
     *     in {@code android.Manifest.permission.*}.
     *     <br>
     *     {@code null} will be returned, if there's no need to ask
     *     of permission.
     * </p>
     *
     * @return the name of the permission or {@code null}
     */
    @Nullable
    public String getPermission() {
        return permission;
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int)} on
     * the provided {@link Activity} with the {@link #getIntent()} and
     * {@link #getRequestCode()} as paramter.
     *
     * @param activity An {@link Activity}
     */
    public void open(@NonNull Activity activity){
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Call {@link Fragment#startActivityForResult(Intent, int)} on
     * the provided {@link Activity} with the {@link #getIntent()} and
     * {@link #getRequestCode()} as paramter.
     *
     * @param fragment A {@link Fragment}
     */
    public void open(@NonNull Fragment fragment) {
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(requestCode);
        dest.writeParcelable(intent, flags);
        dest.writeString(permission);
    }

    public static final Parcelable.Creator<BelvedereIntent> CREATOR
            = new Parcelable.Creator<BelvedereIntent>() {
        public BelvedereIntent createFromParcel(@NonNull Parcel in) {
            return new BelvedereIntent(in);
        }

        @NonNull
        public BelvedereIntent[] newArray(int size) {
            return new BelvedereIntent[size];
        }
    };

    private BelvedereIntent(Parcel in) {
        requestCode = in.readInt();
        intent = in.readParcelable(BelvedereIntent.class.getClassLoader());
        permission = in.readString();
    }
}
