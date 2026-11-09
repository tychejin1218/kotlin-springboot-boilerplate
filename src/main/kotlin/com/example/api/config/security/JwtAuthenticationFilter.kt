package com.example.api.config.security

import com.example.api.common.component.RedisComponent
import com.example.api.common.type.RedisKeys
import com.example.api.config.jwt.JwtTokenProvider
import com.example.api.config.jwt.memberId
import com.example.api.config.jwt.role
import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터
 *
 * `Authorization: Bearer {token}` 을 검증해 SecurityContext 에 인증 정보(principal = [LoginMember])를 넣는다.
 * 로그아웃으로 차단 목록(Redis)에 오른 토큰은 인증하지 않는다.
 *
 * 인증에 성공하면 회원 번호를 MDC([MDC_KEY])에 넣어 이후 로그 줄에 남긴다. 이메일은 개인정보라 남기지 않는다.
 * 바깥의 LoggingFilter 요약 로그에도 찍혀야 하므로 여기서 지우지 않고, 정리는 가장 바깥의 TraceIdFilter 가 맡는다.
 *
 * Spring 이 서블릿 필터로 한 번 더 등록하지 않도록 빈이 아니라 SecurityConfig 에서 직접 만든다.
 */
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisComponent: RedisComponent,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val claims = jwtTokenProvider.getValidClaims(resolveToken(request))
        val loginMember = claims?.takeUnless { isDenied(it) }?.toLoginMember()
        if (loginMember != null) {
            val authorities = listOfNotNull(claims?.role).map(::SimpleGrantedAuthority)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(loginMember, null, authorities)
            MDC.put(MDC_KEY, loginMember.id.toString())
        }
        filterChain.doFilter(request, response)
    }

    /** 회원 번호나 이메일이 없는 토큰은 인증하지 않는다 */
    private fun Claims.toLoginMember(): LoginMember? {
        val id = memberId
        val email = subject
        return if (id != null && email != null) LoginMember(id, email) else null
    }

    /**
     * Redis 장애 시에는 조회 결과가 null 이라 차단하지 않는다 (가용성 우선).
     * 차단 목록은 Access 토큰 유효 기간 안의 짧은 위험만 막는 장치라서 서비스 전체 장애보다 낫다.
     */
    private fun isDenied(claims: Claims): Boolean =
        claims.id?.let { redisComponent.getValue(RedisKeys.ACCESS_TOKEN_DENY.key(it)) } != null

    private fun resolveToken(request: HttpServletRequest): String? =
        request
            .getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)

    companion object {
        /** MDC 키 (logback 패턴의 %X{memberId} 와 같아야 한다) */
        const val MDC_KEY = "memberId"
        const val BEARER_PREFIX = "Bearer "
    }
}
