package com.example.api.config.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class JwtTokenProviderTest {
    private val provider = JwtTokenProvider(properties())

    @Test
    fun `Access 토큰에는 이메일, 회원 번호, 역할이 담긴다`() {
        val claims = provider.getValidClaims(provider.createAccessToken("user@test.com", 7, "ROLE_USER"))

        assertThat(claims?.subject).isEqualTo("user@test.com")
        assertThat(claims?.memberId).isEqualTo(7L)
        assertThat(claims?.role).isEqualTo("ROLE_USER")
        assertThat(claims?.id).isNotBlank()
    }

    @Test
    fun `Refresh 토큰의 만료는 최초 로그인 시각 기준 절대 만료다`() {
        val authTime = Instant.now().minus(Duration.ofHours(1))

        val claims = provider.getValidClaims(provider.createRefreshToken("user@test.com", 7, authTime))

        assertThat(claims?.authTime?.toEpochMilli()).isEqualTo(authTime.toEpochMilli())
        assertThat(
            claims?.expiration?.toInstant(),
        ).isEqualTo(authTime.plusMillis(ABSOLUTE_MS).truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
    }

    @Test
    fun `절대 만료가 지난 Refresh 토큰은 무효다`() {
        val token = provider.createRefreshToken("user@test.com", 7, Instant.now().minusMillis(ABSOLUTE_MS + 1000))

        assertThat(provider.getValidClaims(token)).isNull()
    }

    @Test
    fun `다른 키로 서명했거나 형식이 잘못된 토큰은 무효다`() {
        val otherKey = JwtTokenProvider(properties(secret = "another-secret-key-that-is-at-least-32-bytes-long"))

        assertThat(provider.getValidClaims(otherKey.createAccessToken("user@test.com", 7, "ROLE_USER"))).isNull()
        assertThat(provider.getValidClaims("not-a-jwt")).isNull()
        assertThat(provider.getValidClaims(null)).isNull()
    }

    @Test
    fun `같은 순간에 발급해도 토큰마다 jti 가 달라 서로 다른 토큰이 된다`() {
        val first = provider.createAccessToken("user@test.com", 7, "ROLE_USER")
        val second = provider.createAccessToken("user@test.com", 7, "ROLE_USER")

        assertThat(first).isNotEqualTo(second)
    }

    private fun properties(secret: String = "test-jwt-secret-key-must-be-at-least-256-bits-long") =
        JwtProperties(
            secret = secret,
            accessTokenValidityMs = 60_000,
            sessionIdleTimeoutMs = 1_800_000,
            sessionAbsoluteTimeoutMs = ABSOLUTE_MS,
        )

    companion object {
        private const val ABSOLUTE_MS = 43_200_000L
    }
}
