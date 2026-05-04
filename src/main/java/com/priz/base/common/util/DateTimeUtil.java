package com.priz.base.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeUtil() {}

    public static String formatInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, DEFAULT_ZONE).format(FORMATTER);
    }

    public static Instant now() {
        return Instant.now();
    }
}
