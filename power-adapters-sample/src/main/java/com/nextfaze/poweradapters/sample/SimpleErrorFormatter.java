package com.nextfaze.poweradapters.sample;

import android.content.Context;
import android.support.annotation.Nullable;
import com.nextfaze.asyncdata.widget.ErrorFormatter;
import lombok.NonNull;

final class SimpleErrorFormatter implements ErrorFormatter {
    @Nullable
    @Override
    public String format(@NonNull Context context, @NonNull Throwable e) {
        return "Failed to load: " + e.getMessage();
    }
}