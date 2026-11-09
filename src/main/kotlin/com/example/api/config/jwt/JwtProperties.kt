package com.example.api.config.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 설정 (`jwt.*`)
 *
 * @property secret HS256 서명 키 (32바이트 이상)
 * @property accessTokenValidityMs Access 토큰 유효 기간. [sessionIdleTimeoutMs] 보다 짧아야 미활동 만료가 동작한다
 * @property sessionIdleTimeoutMs 미활동 만료. Refresh 토큰의 Redis TTL 이며 재발급마다 연장된다
 * @property sessionAbsoluteTimeoutMs 최초 로그인 기준 절대 만료. 계속 활동해도 이 시간이 지나면 다시 로그인한다
 */
@ConfigurationProperties("jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidityMs: Long,
    val sessionIdleTimeoutMs: Long,
    val sessionAbsoluteTimeoutMs: Long,
)
