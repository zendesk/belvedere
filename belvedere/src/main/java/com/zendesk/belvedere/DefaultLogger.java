package com.zendesk.belvedere;

import android.util.Log;

/**
 * Logger class that is used, if no custom logger was
 * defined {@link Belvedere.Builder#logger(Logger)}
 * but logging was enabled {@link Belvedere.Builder#debug(boolean)}.
 * <br>
 * Using Android {@link Log} class to print log messages.
 */
class DefaultLogger implements Logger {

    private boolean loggable = false;

    @Override
    public void d(final String tag, final String msg) {
        if(loggable) {
            Log.d(tag, msg);
        }
    }

    @Override
    public void w(final String tag, final String msg) {
        if(loggable) {
            Log.w(tag, msg);
        }
    }

    @Override
    public void e(final String tag, final String msg) {
        if(loggable) {
            Log.e(tag, msg);
        }
    }

    @Override
    public void e(final String tag, final String msg, final Throwable e) {
        if(loggable) {
            Log.e(tag, msg, e);
        }
    }

    @Override
    public void setLoggable(final boolean enabled) {
        this.loggable = enabled;
    }
}