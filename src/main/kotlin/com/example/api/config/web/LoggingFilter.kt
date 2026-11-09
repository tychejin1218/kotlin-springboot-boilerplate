package com.example.api.config.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 요청/응답 요약 로그 필터
 *
 * TraceIdFilter 바로 안쪽에서 실행되어, 요청이 끝난 뒤 메서드·경로·상태·처리 시간을 한 줄로 남긴다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class LoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            log.info("[REQUEST] {} {} → {} ({}ms)", request.method, request.requestURI, response.status, elapsed)
        }
    }
}
