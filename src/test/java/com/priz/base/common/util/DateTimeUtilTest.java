package com.priz.base.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void formatInstant_should_formatInVietnamTimezone() {
        // 2025-01-15T10:30:00Z = 2025-01-15 17:30:00 in Asia/Ho_Chi_Minh (UTC+7)
        Instant instant = Instant.parse("2025-01-15T10:30:00Z");

        String formatted = DateTimeUtil.formatInstant(instant);

        assertEquals("2025-01-15 17:30:00", formatted);
    }

    @Test
    void now_should_returnCurrentInstant() {
        Instant before = Instant.now();
        Instant result = DateTimeUtil.now();
        Instant after = Instant.now();

        assertNotNull(result);
        assertFalse(result.isBefore(before));
        assertFalse(result.isAfter(after));
    }
}
