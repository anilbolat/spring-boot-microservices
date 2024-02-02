package com.anilbolat.shipping.util;

import io.micrometer.tracing.Span;

public class TraceUtils {

    public static void addTag(Span span, String key, String value) {
        if (span != null) {
            span.tag(key, value);
        }
    }

}
