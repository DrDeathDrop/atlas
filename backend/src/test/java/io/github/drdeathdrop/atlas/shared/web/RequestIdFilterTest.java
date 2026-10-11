package io.github.drdeathdrop.atlas.shared.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void aSuppliedIdIsKeptAndVisibleWhileTheRequestRuns() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");
        request.addHeader(RequestIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInside = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenInside.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(seenInside.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void anIdIsGeneratedWhenNoneIsSupplied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).hasSize(36);
    }

    @Test
    void anIdThatCouldForgeLogLinesIsReplaced() {
        assertThat(RequestIdFilter.resolve("abc\nINFO forged line")).hasSize(36);
        assertThat(RequestIdFilter.resolve("x".repeat(65))).hasSize(36);
        assertThat(RequestIdFilter.resolve("")).hasSize(36);
    }

    @Test
    void theIdIsClearedEvenWhenTheRequestFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, response, failing);
        } catch (Exception expected) {
            assertThat(expected).hasMessageContaining("boom");
        }

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
