package zendesk.belvedere;


import android.support.annotation.NonNull;
import android.util.Log;

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


    /**
     * Logger class that is used, if no custom logger was
     * defined {@link Belvedere.Builder#logger(Logger)}
     * but logging was enabled {@link Belvedere.Builder#debug(boolean)}.
     * <br>
     * Using Android {@link Log} class to print log messages.
     */
    static class DefaultLogger implements Logger {

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

    /**
     * Logger interface used in Belvedere.
     * <br>
     * Could be used to pipe log messages into the
     * host apps own logger. To do that implement the methods below
     * and register your instance by calling
     * {@link Belvedere.Builder#logger(Logger)}.
     */
    public interface Logger {

        /**
         * Send a debug log message.
         *
         * @param tag Log message tag.
         * @param msg Log message.
         */
        void d(@NonNull String tag, @NonNull String msg);

        /**
         * Send a warning log message.
         *
         * @param tag Log message tag.
         * @param msg Log message.
         */
        void w(@NonNull String tag, @NonNull String msg);

        /**
         * Send an error log message.
         *
         * @param tag Log message tag.
         * @param msg Log message.
         */
        void e(@NonNull String tag, @NonNull String msg);


        /**
         * Send an error log message with a {@link Throwable}.
         *
         * @param tag Log message tag.
         * @param msg Log message.
         * @param e Throwable
         */
        void e(@NonNull String tag, @NonNull String msg, @NonNull Throwable e);

        /**
         * Enable/disable logging.
         *
         * @param enabled True if logging should be enabled,
         *                False if not.
         */
        void setLoggable(boolean enabled);
    }

}
