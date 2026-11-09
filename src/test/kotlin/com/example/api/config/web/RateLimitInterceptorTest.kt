package com.example.api.config.web

import com.example.api.common.component.RedisComponent
import com.example.api.common.type.RedisKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper

class RateLimitInterceptorTest {
    private val redisComponent = mockk<RedisComponent>()

    @Test
    fun `한도 안의 요청은 통과시킨다`() {
        every { redisComponent.increment(any(), any()) } returns 10

        assertThat(interceptor().preHandle(request(), MockHttpServletResponse(), Any())).isTrue()
    }

    @Test
    fun `한도를 넘으면 429 와 SYSTEM_RATE_LIMIT_EXCEEDED 로 응답한다`() {
        every { redisComponent.increment(any(), any()) } returns 11
        val response = MockHttpServletResponse()

        assertThat(interceptor().preHandle(request(), response, Any())).isFalse()
        assertThat(response.status).isEqualTo(429)
        assertThat(response.contentAsString).contains("\"statusCode\":\"SYSTEM_RATE_LIMIT_EXCEEDED\"")
    }

    @Test
    fun `Redis 장애로 셀 수 없으면 막지 않는다`() {
        every { redisComponent.increment(any(), any()) } returns null

        assertThat(interceptor().preHandle(request(), MockHttpServletResponse(), Any())).isTrue()
    }

    @Test
    fun `X-Forwarded-For 는 신뢰하도록 켰을 때만 클라이언트 IP 로 쓴다`() {
        every { redisComponent.increment(any(), any()) } returns 1
        val request = request().apply { addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1") }

        interceptor(trustForwardedHeaders = false).preHandle(request, MockHttpServletResponse(), Any())
        interceptor(trustForwardedHeaders = true).preHandle(request, MockHttpServletResponse(), Any())

        verify { redisComponent.increment(RedisKeys.RATE_LIMIT.key("127.0.0.1", "/auth/login"), any()) }
        verify { redisComponent.increment(RedisKeys.RATE_LIMIT.key("203.0.113.7", "/auth/login"), any()) }
    }

    private fun interceptor(trustForwardedHeaders: Boolean = false) =
        RateLimitInterceptor(
            redisComponent,
            JsonMapper.builder().build(),
            RateLimitProperties(10, trustForwardedHeaders),
        )

    private fun request() = MockHttpServletRequest("POST", "/auth/login").apply { remoteAddr = "127.0.0.1" }
}
