package com.example.api.config.web

import com.example.api.common.component.RedisComponent
import com.example.api.common.response.ErrorResponse
import com.example.api.common.type.ApiStatus
import com.example.api.common.type.RedisKeys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.web.servlet.HandlerInterceptor
import tools.jackson.databind.ObjectMapper

/**
 * Rate Limit 설정 (`rate-limit.*`)
 *
 * @property maxRequestsPerMinute IP·경로마다 1분에 허용하는 요청 수
 * @property trustForwardedHeaders true 면 X-Forwarded-For 첫 값을 클라이언트 IP 로 쓴다.
 *   누구나 보낼 수 있는 헤더라서 신뢰할 수 있는 리버스 프록시(ALB, nginx) 뒤에서만 켠다
 */
@ConfigurationProperties("rate-limit")
data class RateLimitProperties(
    val maxRequestsPerMinute: Long = 10,
    val trustForwardedHeaders: Boolean = false,
)

/**
 * Rate Limit 인터셉터
 *
 * IP + 경로마다 1분 동안의 요청 수를 Redis 카운터로 세고, 넘으면 429(SYSTEM_RATE_LIMIT_EXCEEDED)로 응답한다.
 * Redis 장애로 셀 수 없으면 막지 않는다(가용성 우선). 적용 경로는 WebConfig 에서 정한다.
 */
class RateLimitInterceptor(
    private val redisComponent: RedisComponent,
    private val objectMapper: ObjectMapper,
    private val properties: RateLimitProperties,
) : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val ip = resolveClientIp(request)
        val window = checkNotNull(RedisKeys.RATE_LIMIT.ttl)
        val count = redisComponent.increment(RedisKeys.RATE_LIMIT.key(ip, request.requestURI), window)
        if (count == null || count <= properties.maxRequestsPerMinute) return true

        log.warn("[RateLimit] 요청 초과 - uri: {}, count: {}", request.requestURI, count)
        val apiStatus = ApiStatus.SYSTEM_RATE_LIMIT_EXCEEDED
        response.status = apiStatus.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, ErrorResponse.of(apiStatus, request.method, request.requestURI))
        return false
    }

    private fun resolveClientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.takeIf { properties.trustForwardedHeaders }
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr
}
