package com.zendesk.belvedere.ui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Belvedere's own {@link SharedPreferences}. Used to store
 * information about permission handling.
 */
class BelvedereSharedPreferences {

    private static final String BELVEDERE_SHARED_PREFERENCES = "belvedere_prefs";
    private final SharedPreferences sharedPreferences;

    BelvedereSharedPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(BELVEDERE_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }

    /**
     * Should {@link BelvedereDialog} ask for the specified permission?
     *
     * @param permission Name of the permission
     * @return {@code true} don't bother the user,
     *     {@code false} go ahead and ask
     */
    boolean shouldINeverEverAskForThatPermissionAgain(String permission) {
        return sharedPreferences.contains(permission);
    }

    /**
     * Store the specified permission to never ever ask the user again.
     *
     * @param permission name of the permission
     */
    void neverEverAskForThatPermissionAgain(String permission) {
        sharedPreferences
                .edit()
                .putBoolean(permission, true)
                .apply();
    }
}
