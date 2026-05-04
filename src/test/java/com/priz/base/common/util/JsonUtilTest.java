package com.priz.base.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void toJson_should_serializeObject() {
        Map<String, String> data = Map.of("key", "value");

        String json = JsonUtil.toJson(data);

        assertNotNull(json);
        assertTrue(json.contains("\"key\""));
        assertTrue(json.contains("\"value\""));
    }

    @Test
    void toJson_should_returnEmptyObject_onFailure() {
        // Self-referencing object that cannot be serialized
        Object problematic = new Object() {
            @SuppressWarnings("unused")
            public Object getSelf() { return this; }
        };

        String json = JsonUtil.toJson(problematic);

        assertEquals("{}", json);
    }

    @Test
    void fromJson_should_deserializeValidJson() {
        String json = "{\"key\":\"value\"}";

        @SuppressWarnings("unchecked")
        Map<String, String> result = JsonUtil.fromJson(json, Map.class);

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    void fromJson_should_returnNull_onInvalidJson() {
        String invalidJson = "not valid json {{{";

        Object result = JsonUtil.fromJson(invalidJson, Map.class);

        assertNull(result);
    }

    @Test
    void toJson_should_handleInstant_asIsoString() {
        Map<String, Instant> data = Map.of("timestamp", Instant.parse("2025-01-15T10:30:00Z"));

        String json = JsonUtil.toJson(data);

        assertNotNull(json);
        assertTrue(json.contains("2025-01-15T10:30:00Z"));
        assertFalse(json.matches(".*\\d{10,}.*")); // should not be epoch timestamp
    }
}
