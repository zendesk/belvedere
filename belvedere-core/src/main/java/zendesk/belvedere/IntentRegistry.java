package zendesk.belvedere;

import android.util.SparseArray;

class IntentRegistry {

    final static int PLACE_HOLDER_CODE = 42;

    private final static int START_REQUEST_CODE = 1600;
    private final static int END_REQUEST_CODE = 1700;

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
        if(requestCode == PLACE_HOLDER_CODE) {
            return MediaResult.empty();
        }

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
