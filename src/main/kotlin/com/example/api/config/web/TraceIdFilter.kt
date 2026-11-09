package com.example.api.config.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청 추적 ID 필터
 *
 * 요청마다 traceId 를 MDC 에 넣어 같은 요청에서 남긴 모든 로그 줄에 같은 ID 가 찍히게 하고,
 * 응답 헤더 [HEADER] 로도 돌려준다. 앞단(게이트웨이, 다른 서비스)이 보낸 [HEADER] 가 형식에 맞으면 이어서 쓴다.
 *
 * 가장 바깥 필터라서 요청이 끝날 때 MDC 정리도 여기서 한다.
 * 톰캣은 스레드를 재사용하므로 정리하지 않으면 다음 요청 로그에 이전 요청의 값이 섞인다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = resolveTraceId(request.getHeader(HEADER))
        MDC.put(MDC_KEY, traceId)
        response.setHeader(HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    private fun resolveTraceId(incoming: String?): String =
        incoming?.takeIf { VALID_TRACE_ID.matches(it) }
            ?: UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(GENERATED_LENGTH)

    companion object {
        /** 추적 ID 를 주고받는 HTTP 헤더 */
        const val HEADER = "X-Trace-Id"

        /** MDC 키 (logback 패턴의 %X{traceId} 와 같아야 한다) */
        const val MDC_KEY = "traceId"

        /** 외부에서 받은 ID 는 로그 주입을 막기 위해 영숫자와 하이픈, 64자 이하만 허용한다 */
        private val VALID_TRACE_ID = Regex("^[A-Za-z0-9-]{1,64}$")

        private const val GENERATED_LENGTH = 16
    }
}
