package com.zendesk.belvedere;

import android.util.SparseArray;

import java.io.File;

class IntentRegistry {

    final static File DUMMY_FILE = new File("/");

    private final static int START_REQUEST_CODE = 1600;
    private final static int END_REQUEST_CODE = 1700;

    private SparseArray<BelvedereResult> pendingIntents;

    IntentRegistry() {
        this.pendingIntents = new SparseArray<>(END_REQUEST_CODE - START_REQUEST_CODE);
    }

    int reserveSlot() {
        final int requestCode;
        synchronized (this) {
            requestCode = getRequestCode();
            pendingIntents.put(requestCode, BelvedereResult.empty());
        }
        return requestCode;
    }

    void freeSlot(int requestCode) {
        synchronized (this) {
            pendingIntents.remove(requestCode);
        }
    }

    void updateRequestCode(int requestCode, BelvedereResult belvedereResult) {
        synchronized (this) {
            pendingIntents.put(requestCode, belvedereResult);
        }
    }

    BelvedereResult getForRequestCode(int requestCode) {
        synchronized (this) {
            return pendingIntents.get(requestCode);
        }
    }

    private int getRequestCode() {
        for(int i = START_REQUEST_CODE; i < END_REQUEST_CODE; i++) {
            if(pendingIntents.get(i) == null) {
                return i;
            }
        }

        return -1;
    }
}
