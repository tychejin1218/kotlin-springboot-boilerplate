package com.example.api.auth.service

import com.example.api.auth.dto.RefreshRequest
import com.example.api.common.component.RedisComponent
import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.common.type.RedisKeys
import com.example.api.config.jwt.JwtProperties
import com.example.api.config.jwt.JwtTokenProvider
import com.example.api.config.jwt.authTime
import com.example.api.domain.entity.Member
import com.example.api.domain.repository.MemberRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.Duration
import java.time.Instant
import java.util.Optional

class TokenServiceTest {
    private val jwtTokenProvider =
        JwtTokenProvider(
            JwtProperties("test-jwt-secret-key-must-be-at-least-256-bits-long", 60_000, 1_800_000, 43_200_000),
        )
    private val redisComponent = mockk<RedisComponent>(relaxed = true)
    private val memberRepository = mockk<MemberRepository>()
    private val tokenService = TokenService(jwtTokenProvider, redisComponent, memberRepository)
    private val member =
        Member(email = EMAIL, name = "홍길동", password = "x").also {
            ReflectionTestUtils.setField(it, "id", 1L)
        }

    @Test
    fun `발급하면 Refresh 토큰을 회원 번호 키로 미활동 만료 TTL 과 함께 저장한다`() {
        val tokens = tokenService.issue(member)

        verify { redisComponent.setValue(REFRESH_KEY, tokens.refreshToken, jwtTokenProvider.sessionIdleTimeout) }
    }

    @Test
    fun `재발급하면 최초 로그인 시각을 이어받은 새 토큰으로 교체한다`() {
        val authTime = Instant.now().minus(Duration.ofHours(1))
        val token = jwtTokenProvider.createRefreshToken(EMAIL, 1, authTime)
        every { redisComponent.getValue(REFRESH_KEY) } returns token
        every { memberRepository.findById(1) } returns Optional.of(member)
        every { redisComponent.compareAndSet(REFRESH_KEY, token, any(), any()) } returns true

        val tokens = tokenService.refresh(RefreshRequest(token))

        assertThat(tokens.refreshToken).isNotEqualTo(token)
        assertThat(
            jwtTokenProvider.getValidClaims(tokens.refreshToken)?.authTime?.toEpochMilli(),
        ).isEqualTo(authTime.toEpochMilli())
    }

    @Test
    fun `Redis 에 저장된 것과 다른 Refresh 토큰이 오면 재사용으로 보고 세션을 끊는다`() {
        val oldToken = jwtTokenProvider.createRefreshToken(EMAIL, 1, Instant.now())
        every { redisComponent.getValue(REFRESH_KEY) } returns "newer-token"

        assertRefreshInvalid { tokenService.refresh(RefreshRequest(oldToken)) }
        verify { redisComponent.delete(REFRESH_KEY) }
    }

    @Test
    fun `Redis 에 Refresh 토큰이 없으면 미활동 만료로 재발급에 실패한다`() {
        val token = jwtTokenProvider.createRefreshToken(EMAIL, 1, Instant.now())
        every { redisComponent.getValue(REFRESH_KEY) } returns null

        assertRefreshInvalid { tokenService.refresh(RefreshRequest(token)) }
    }

    @Test
    fun `동시에 다른 요청이 먼저 교체했으면 재발급에 실패한다`() {
        val token = jwtTokenProvider.createRefreshToken(EMAIL, 1, Instant.now())
        every { redisComponent.getValue(REFRESH_KEY) } returns token
        every { memberRepository.findById(1) } returns Optional.of(member)
        every { redisComponent.compareAndSet(any(), any(), any(), any()) } returns false

        assertRefreshInvalid { tokenService.refresh(RefreshRequest(token)) }
    }

    @Test
    fun `폐기하면 Refresh 토큰을 지우고 Access 토큰을 남은 시간 동안 차단한다`() {
        val tokens = tokenService.issue(member)
        val jti = checkNotNull(jwtTokenProvider.getValidClaims(tokens.accessToken)?.id)

        tokenService.revoke(tokens.refreshToken, tokens.accessToken)

        verify { redisComponent.delete(REFRESH_KEY) }
        verify { redisComponent.setValue(RedisKeys.ACCESS_TOKEN_DENY.key(jti), "1", match { it > Duration.ZERO }) }
    }

    @Test
    fun `잘못된 토큰으로 폐기해도 예외 없이 끝난다`() {
        tokenService.revoke("broken", "broken")

        verify(exactly = 0) { redisComponent.delete(any()) }
    }

    private fun assertRefreshInvalid(call: () -> Unit) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException::class.java) {
            assertThat(it.apiStatus).isEqualTo(ApiStatus.AUTH_REFRESH_TOKEN_INVALID)
        }
    }

    companion object {
        private const val EMAIL = "user@test.com"
        private val REFRESH_KEY = RedisKeys.REFRESH_TOKEN.key(1L)
    }
}
