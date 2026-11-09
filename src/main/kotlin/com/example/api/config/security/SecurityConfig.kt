package com.example.api.config.security

import com.example.api.common.component.RedisComponent
import com.example.api.common.response.ErrorResponse
import com.example.api.common.type.ApiStatus
import com.example.api.config.jwt.JwtTokenProvider
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher
import tools.jackson.databind.ObjectMapper

/**
 * Spring Security 설정
 *
 * 세션 없이(STATELESS) JWT 로만 인증한다. 인증 실패(401)·권한 거부(403)도 ErrorResponse JSON 으로 응답한다.
 * CORS 는 WebConfig 의 MVC 설정을 그대로 쓴다.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisComponent: RedisComponent,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            exceptionHandling {
                authenticationEntryPoint =
                    { request, response, _ -> writeError(request, response, ApiStatus.AUTH_UNAUTHORIZED) }
                accessDeniedHandler =
                    { request, response, _ -> writeError(request, response, ApiStatus.PERMISSION_FORBIDDEN) }
            }
            authorizeHttpRequests {
                // SSE 등 비동기 응답의 ASYNC 디스패치와 오류 페이지 디스패치는 처음 요청에서 이미 인가했다
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ASYNC), permitAll)
                authorize(DispatcherTypeRequestMatcher(DispatcherType.ERROR), permitAll)
                PERMIT_ALL_PATTERNS.forEach { authorize(it, permitAll) }
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(
                JwtAuthenticationFilter(jwtTokenProvider, redisComponent),
            )
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    private fun writeError(
        request: HttpServletRequest,
        response: HttpServletResponse,
        apiStatus: ApiStatus,
    ) {
        response.status = apiStatus.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, ErrorResponse.of(apiStatus, request.method, request.requestURI))
    }

    companion object {
        /** 인증 없이 호출할 수 있는 경로 (context-path /api 제외) */
        val PERMIT_ALL_PATTERNS =
            listOf(
                "/",
                "/health",
                "/error",
                "/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/**",
                "/auth/register",
                "/auth/verify-email",
                "/auth/resend-verification",
                "/auth/login",
                "/auth/logout",
                "/auth/token/refresh",
                "/auth/token/temp",
                "/auth/password/reset-request",
                "/auth/password/reset",
            )
    }
}
