package com.nextfaze.poweradapters.sample;

import lombok.NonNull;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class NewsSection {

    @NonNull
    private final String mTitle;

    public NewsSection(@NonNull String title) {
        mTitle = title;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String toString() {
        return mTitle;
    }
}
