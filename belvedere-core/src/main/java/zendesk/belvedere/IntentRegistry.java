package zendesk.belvedere;

import android.util.SparseArray;

class IntentRegistry {

    private final static int START_REQUEST_CODE = 1600;
    private final static int END_REQUEST_CODE = 1650;

    private SparseArray<MediaResult> pendingIntents;

    IntentRegistry() {
        this.pendingIntents = new SparseArray<>();
    }

    int reserveSlot() {
        final int requestCode;
        synchronized (this) {
            requestCode = getRequestCode();
            pendingIntents.put(requestCode, MediaResult.empty());
        }
        return requestCode;
    }

    void freeSlot(int requestCode) {
        synchronized (this) {
            pendingIntents.remove(requestCode);
        }
    }

    void updateRequestCode(int requestCode, MediaResult belvedereResult) {
        synchronized (this) {
            pendingIntents.put(requestCode, belvedereResult);
        }
    }

    MediaResult getForRequestCode(int requestCode) {
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

        L.d(Belvedere.LOG_TAG, "No slot free. Clearing registry.");
        pendingIntents.clear();
        return getRequestCode();
    }
}
