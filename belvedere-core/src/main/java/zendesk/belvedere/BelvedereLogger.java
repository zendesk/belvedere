package zendesk.belvedere;

import android.support.annotation.NonNull;

/**
 * Logger interface used in Belvedere.
 * <br>
 * Could be used to pipe log messages into the
 * host apps own logger. To do that implement the methods below
 * and register your instance by calling
 * {@link BelvedereConfig.Builder#withCustomLogger(BelvedereLogger)}.
 *
 */
public interface BelvedereLogger {

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
