package com.priz.base.infrastructure.security.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CachedBodyHttpServletRequestTest {

    @Test
    void getCachedBodyAsString_returnsOriginalBody() throws Exception {
        String body = "{\"name\":\"test\",\"value\":42}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

        assertThat(cached.getCachedBodyAsString()).isEqualTo(body);
    }

    @Test
    void getInputStream_canBeReadMultipleTimes() throws Exception {
        String body = "hello world";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

        String first = new String(cached.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String second = new String(cached.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(first).isEqualTo(body);
        assertThat(second).isEqualTo(body);
    }

    @Test
    void getCachedBodyAsString_emptyBody_returnsEmptyString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(new byte[0]);

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

        assertThat(cached.getCachedBodyAsString()).isEmpty();
    }

    @Test
    void setAttribute_getAttribute_roundtrips() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

        cached.setAttribute("myKey", "myValue");

        assertThat(cached.getAttribute("myKey")).isEqualTo("myValue");
    }

    @Test
    void getReader_andGetInputStream_returnSameContent() throws Exception {
        String body = "line one\nline two";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

        String viaInputStream = new String(cached.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        StringBuilder viaReader = new StringBuilder();
        try (var reader = cached.getReader()) {
            reader.lines().forEach(l -> {
                if (!viaReader.isEmpty()) viaReader.append('\n');
                viaReader.append(l);
            });
        }

        assertThat(viaInputStream).isEqualTo(body);
        assertThat(viaReader.toString()).isEqualTo(body);
    }
}
