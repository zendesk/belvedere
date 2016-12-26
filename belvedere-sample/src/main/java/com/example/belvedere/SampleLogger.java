package com.example.belvedere;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.zendesk.belvedere.Logger;

import java.util.Locale;

class SampleLogger implements Logger {

    private TextView textView;
    private boolean loggable;

    private final int debugColor;
    private final int warnColor;
    private final int errorColor;

    SampleLogger(TextView textView) {
        this.textView = textView;

        debugColor = ContextCompat.getColor(textView.getContext(), R.color.belvedereSecondaryText);
        warnColor = ContextCompat.getColor(textView.getContext(), R.color.sample_logger_warn);
        errorColor = ContextCompat.getColor(textView.getContext(), R.color.sample_logger_error);
    }

    @Override
    public void d(@NonNull final String tag, @NonNull final String msg) {
        Log.d(tag, msg);
        internalAppend(tag, msg, debugColor);
    }

    @Override
    public void w(@NonNull final String tag, @NonNull final String msg) {
        Log.w(tag, msg);
        internalAppend(tag, msg, warnColor);
    }

    @Override
    public void e(@NonNull final String tag, @NonNull final String msg) {
        Log.e(tag, msg);
        internalAppend(tag, msg, errorColor);
    }

    @Override
    public void e(@NonNull final String tag, @NonNull final String msg, @NonNull final Throwable e) {
        Log.e(tag, msg, e);
        internalAppend(tag, String.format(Locale.US, "%s\n%s", msg, e.getLocalizedMessage()), errorColor);
    }

    @Override
    public void setLoggable(final boolean enabled) {
        this.loggable = enabled;
    }

    private void internalAppend(final String tag, final String msg, final int color) {
        if (!loggable) return;

        textView.setVisibility(View.VISIBLE);

        final Spannable text = new SpannableString(String.format(Locale.US, "%s - %s", tag, msg));
        text.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Spannable newLine = new SpannableString("\n \n");
        newLine.setSpan(new RelativeSizeSpan(0.5f), 0, newLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final CharSequence text1 = textView.getText();
                textView.setText(text);
                textView.append(newLine);
                textView.append(text1);
            }
        });
    }
}