package com.example.api.support

import com.example.api.common.component.RedisComponent
import com.example.api.config.jwt.JwtTokenProvider
import com.example.api.config.security.SecurityConfig
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

/**
 * `@WebMvcTest` 에 실제 보안 설정(인증 예외 경로, 401/403 JSON 응답)을 적용한다
 *
 * 슬라이스 테스트는 SecurityConfig 를 읽지 않아 Spring Boot 기본 보안(모든 요청 인증, CSRF)이 걸린다.
 * 인증된 요청은 `@WithMockUser(username = "이메일")` 로 만든다. Redis 는 목으로 바꾼다(차단 목록 없음).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SecurityConfig::class, JwtTokenProvider::class, SecurityTestConfig::class)
annotation class ImportSecurityConfig

@TestConfiguration(proxyBeanMethods = false)
class SecurityTestConfig {
    @Bean
    fun redisComponent(): RedisComponent = mockk { every { getValue(any()) } returns null }
}
