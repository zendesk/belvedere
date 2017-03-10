package zendesk.belvedere;


import android.support.annotation.NonNull;

class L {

    private static Logger logger = new DefaultLogger();

    static void setLogger(Logger logger) {
        L.logger = logger;
    }

    static void d(@NonNull String tag, @NonNull String msg) {
        logger.d(tag, msg);
    }

    static void w(@NonNull String tag, @NonNull String msg) {
        logger.w(tag, msg);
    }

    static void e(@NonNull String tag, @NonNull String msg) {
        logger.e(tag, msg);
    }

    static void e(@NonNull String tag, @NonNull String msg, @NonNull Throwable e) {
        logger.e(tag, msg, e);
    }

}
