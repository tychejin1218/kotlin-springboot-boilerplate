package com.example.api.config.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * JWT 생성 / 검증
 *
 * 세션 만료 정책:
 * - 미활동 만료 (`jwt.session-idle-timeout-ms`): Refresh 토큰의 Redis TTL. 재발급마다 연장되므로 그 시간 동안 요청이 없으면 세션이 끝난다.
 * - 절대 만료 (`jwt.session-absolute-timeout-ms`): Refresh 토큰의 exp 를 최초 로그인 시각(authTime) + 절대 만료로 잡는다.
 * - Access 토큰 유효 기간은 미활동 만료보다 짧아야 한다. 길면 Access 토큰만으로 계속 요청할 수 있어 미활동 만료가 의미 없어진다.
 *
 * subject 는 이메일, `mid` claim 은 회원 번호다. 로그와 Redis 키에는 개인정보 대신 회원 번호를 쓴다.
 */
@Component
@EnableConfigurationProperties(JwtProperties::class)
class JwtTokenProvider(
    private val properties: JwtProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(properties.secret.toByteArray())

    /** Refresh 토큰 Redis TTL */
    val sessionIdleTimeout: Duration = Duration.ofMillis(properties.sessionIdleTimeoutMs)

    init {
        if (properties.accessTokenValidityMs >= properties.sessionIdleTimeoutMs) {
            log.warn(
                "[JWT] access-token-validity-ms({}) 가 session-idle-timeout-ms({}) 보다 길거나 같아 미활동 만료가 동작하지 않습니다.",
                properties.accessTokenValidityMs,
                properties.sessionIdleTimeoutMs,
            )
        }
    }

    fun createAccessToken(
        email: String,
        memberId: Long,
        role: String,
    ): String {
        val now = Instant.now()
        return baseBuilder(email, memberId, now)
            .claim(CLAIM_ROLE, role)
            .expiration(Date.from(now.plusMillis(properties.accessTokenValidityMs)))
            .compact()
    }

    /**
     * exp 는 발급 시각이 아니라 [authTime] + 절대 만료다.
     * 재발급할 때 기존 토큰의 authTime 을 그대로 넘겨야 절대 만료가 유지된다.
     */
    fun createRefreshToken(
        email: String,
        memberId: Long,
        authTime: Instant,
    ): String =
        baseBuilder(email, memberId, Instant.now())
            .claim(CLAIM_AUTH_TIME, authTime.toEpochMilli())
            .expiration(Date.from(authTime.plusMillis(properties.sessionAbsoluteTimeoutMs)))
            .compact()

    /** 서명과 만료를 검증한 Claims. 무효하면 null */
    fun getValidClaims(token: String?): Claims? {
        if (token.isNullOrBlank()) return null
        return try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            log.warn("[JWT] 유효하지 않은 토큰: {} - {}", e.javaClass.simpleName, e.message)
            null
        } catch (e: IllegalArgumentException) {
            log.warn("[JWT] 빈 토큰: {}", e.message)
            null
        }
    }

    /**
     * jti(토큰 고유 ID)를 넣은 공통 빌더.
     * jti 가 없으면 같은 초에 발급한 토큰이 완전히 같아져 Refresh 토큰 재사용 감지와 Access 토큰 차단 목록이 구분하지 못한다.
     */
    private fun baseBuilder(
        email: String,
        memberId: Long,
        issuedAt: Instant,
    ): JwtBuilder =
        Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim(CLAIM_MEMBER_ID, memberId)
            .issuedAt(Date.from(issuedAt))
            .signWith(secretKey)

    companion object {
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_MEMBER_ID = "mid"
        private const val CLAIM_AUTH_TIME = "authTime"
    }
}

val Claims.role: String?
    get() = get("role", String::class.java)

val Claims.memberId: Long?
    get() = get("mid", Number::class.java)?.toLong()

/** 최초 로그인 시각 (Refresh 토큰에만 있다) */
val Claims.authTime: Instant?
    get() = get("authTime", Number::class.java)?.let { Instant.ofEpochMilli(it.toLong()) }
