package com.example.api.config.web

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TraceIdFilterTest {
    private val filter = TraceIdFilter()

    @Test
    fun `헤더가 없으면 16자리 추적 ID 를 만들어 MDC 와 응답 헤더에 넣는다`() {
        var traceIdInChain: String? = null
        val response = MockHttpServletResponse()

        filter.doFilter(
            MockHttpServletRequest(),
            response,
            FilterChain { _, _ ->
                traceIdInChain =
                    MDC.get(TraceIdFilter.MDC_KEY)
            },
        )

        assertThat(traceIdInChain).hasSize(16)
        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo(traceIdInChain)
    }

    @Test
    fun `형식에 맞는 헤더 값은 그대로 이어서 쓴다`() {
        val request = MockHttpServletRequest().apply { addHeader(TraceIdFilter.HEADER, "gateway-trace-1") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("gateway-trace-1")
    }

    @Test
    fun `로그 주입이 가능한 헤더 값은 버리고 새로 만든다`() {
        val request = MockHttpServletRequest().apply { addHeader(TraceIdFilter.HEADER, "bad\nINFO fake log") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertThat(response.getHeader(TraceIdFilter.HEADER)).hasSize(16).doesNotContain("bad")
    }

    @Test
    fun `요청이 끝나면 MDC 를 비운다`() {
        filter.doFilter(
            MockHttpServletRequest(),
            MockHttpServletResponse(),
            FilterChain {
                _,
                _,
                ->
                MDC.put("memberId", "1")
            },
        )

        assertThat(MDC.getCopyOfContextMap().orEmpty()).isEmpty()
    }
}
