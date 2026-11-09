package com.example.api.auth.service

import com.example.api.auth.dto.RefreshRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.common.component.RedisComponent
import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.common.type.RedisKeys
import com.example.api.config.jwt.JwtTokenProvider
import com.example.api.config.jwt.authTime
import com.example.api.config.jwt.memberId
import com.example.api.domain.entity.Member
import com.example.api.domain.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * 토큰 발급 · 재발급 · 폐기
 *
 * Refresh 토큰은 회원 번호별로 Redis 에 하나만 저장한다(`APP:AUTH:REFRESH_TOKEN:{회원 번호}`).
 * 재발급할 때마다 새 토큰으로 바꾸고(Rotation), 이미 쓴 토큰이 다시 오면 탈취로 보고 세션을 끊는다.
 */
@Service
@Transactional(readOnly = true)
class TokenService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisComponent: RedisComponent,
    private val memberRepository: MemberRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 새 세션을 연다. 지금이 최초 로그인 시각(authTime)이 된다 */
    fun issue(member: Member): TokenResponse {
        val tokens = createTokenPair(member, Instant.now())
        redisComponent.setValue(
            RedisKeys.REFRESH_TOKEN.key(member.requireId()),
            tokens.refreshToken,
            jwtTokenProvider.sessionIdleTimeout,
        )
        return tokens
    }

    /**
     * 토큰 재발급 (Refresh Token Rotation)
     *
     * 1. JWT 검증: 서명과 exp(= 최초 로그인 + 절대 만료)
     * 2. Redis 저장값 확인: 없으면 미활동 만료 또는 로그아웃, 다르면 이미 쓴 토큰의 재사용(탈취 의심)
     * 3. authTime 을 이어받아 새 토큰 쌍 발급 (절대 만료 유지)
     * 4. compare-and-set 으로 교체: 같은 토큰으로 동시에 들어온 요청 중 하나만 성공하고, TTL 은 다시 미활동 만료만큼 연장된다
     */
    fun refresh(request: RefreshRequest): TokenResponse {
        val refreshToken = request.refreshToken
        val claims = jwtTokenProvider.getValidClaims(refreshToken)
        val authTime = claims?.authTime
        val member =
            claims
                ?.memberId
                ?.takeIf { isCurrentRefreshToken(it, refreshToken) }
                ?.let { memberRepository.findByIdOrNull(it) }
        if (member == null || authTime == null) throw ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID)

        val tokens = createTokenPair(member, authTime)
        val key = RedisKeys.REFRESH_TOKEN.key(member.requireId())
        if (!redisComponent.compareAndSet(
                key,
                refreshToken,
                tokens.refreshToken,
                jwtTokenProvider.sessionIdleTimeout,
            )
        ) {
            // 같은 토큰으로 동시에 들어온 다른 요청이 먼저 교체했거나 그 사이 만료됨
            throw ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID)
        }
        return tokens
    }

    /**
     * Refresh 토큰을 지워 재발급을 막고, Access 토큰은 남은 유효 시간 동안 차단 목록에 올린다.
     * 토큰이 이미 만료됐거나 잘못돼도 실패로 응답하지 않는다(사용자 입장에서 로그아웃은 항상 성공해야 한다).
     */
    fun revoke(
        refreshToken: String,
        accessToken: String?,
    ) {
        jwtTokenProvider.getValidClaims(refreshToken)?.memberId?.let {
            redisComponent.delete(RedisKeys.REFRESH_TOKEN.key(it))
        }
        denyAccessToken(accessToken)
    }

    /** Redis 에 저장된 Refresh 토큰과 같은지 확인. 다르면 재사용(탈취 의심)으로 보고 세션을 끊는다 */
    private fun isCurrentRefreshToken(
        memberId: Long,
        refreshToken: String,
    ): Boolean {
        val key = RedisKeys.REFRESH_TOKEN.key(memberId)
        val stored = redisComponent.getValue(key) ?: return false
        val current = stored == refreshToken
        if (!current) {
            log.warn("Refresh 토큰 재사용 감지: 세션 무효화")
            redisComponent.delete(key)
        }
        return current
    }

    private fun createTokenPair(
        member: Member,
        authTime: Instant,
    ): TokenResponse {
        val memberId = member.requireId()
        return TokenResponse(
            accessToken = jwtTokenProvider.createAccessToken(member.email, memberId, member.role.name),
            refreshToken = jwtTokenProvider.createRefreshToken(member.email, memberId, authTime),
        )
    }

    /** jti 를 키로 남은 유효 시간 동안 차단한다. JwtAuthenticationFilter 가 매 요청 확인한다 */
    private fun denyAccessToken(accessToken: String?) {
        val claims = jwtTokenProvider.getValidClaims(accessToken) ?: return
        val jti = claims.id ?: return
        val remaining = Duration.between(Instant.now(), claims.expiration.toInstant())
        if (remaining > Duration.ZERO) {
            redisComponent.setValue(RedisKeys.ACCESS_TOKEN_DENY.key(jti), "1", remaining)
        }
    }

    private fun Member.requireId(): Long = checkNotNull(id) { "저장되지 않은 회원에게는 토큰을 발급할 수 없습니다." }
}
