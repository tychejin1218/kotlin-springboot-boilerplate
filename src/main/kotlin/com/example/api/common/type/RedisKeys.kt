package com.example.api.common.type

import java.time.Duration

/**
 * Redis 키와 TTL
 *
 * 키 형식: `{NAMESPACE}:{도메인}:{용도}:{식별자...}` (예: `APP:AUTH:REFRESH_TOKEN:3`).
 * 키는 반드시 [key] 로 만든다. 문자열을 직접 이어 붙이면 구분자가 빠지거나 겹치기 쉽다.
 *
 * - [NAMESPACE]: 서비스 이름. 이 보일러플레이트로 새 서비스를 만들면 서비스 이름으로 바꾼다.
 * - 식별자 안의 `:` 는 `_` 로 바꾼다. IPv6 주소처럼 값에 콜론이 있으면 어디까지가 값인지 구분할 수 없기 때문이다.
 * - 식별자에는 이메일 같은 개인정보 대신 회원 번호를 쓴다. 키는 모니터링 화면과 로그에 그대로 보인다.
 *
 * @property ttl 고정 TTL. null 이면 코드에서 계산한다
 */
enum class RedisKeys(
    private val path: String,
    val ttl: Duration?,
) {
    /** {회원 번호}, TTL = jwt.session-idle-timeout-ms */
    REFRESH_TOKEN("AUTH:REFRESH_TOKEN", null),

    /** {jti}, TTL = Access 토큰 남은 유효 시간 */
    ACCESS_TOKEN_DENY("AUTH:ACCESS_TOKEN_DENY", null),

    /** {인증 토큰} */
    EMAIL_VERIFY_TOKEN("AUTH:EMAIL_VERIFY", Duration.ofHours(24)),

    /** {재설정 토큰} */
    PASSWORD_RESET_TOKEN("AUTH:PASSWORD_RESET", Duration.ofMinutes(30)),

    /** {IP}:{URI} */
    RATE_LIMIT("RATE_LIMIT", Duration.ofMinutes(1)),
    ;

    /** `APP:{path}:{id1}:{id2}...`. null 식별자는 빈 문자열 */
    fun key(vararg ids: Any?): String {
        val prefix = NAMESPACE + DELIMITER + path
        if (ids.isEmpty()) return prefix
        return prefix + DELIMITER +
            ids.joinToString(DELIMITER) { it?.toString().orEmpty().replace(DELIMITER, ESCAPED_DELIMITER) }
    }

    companion object {
        /** 서비스 이름 접두사. 새 서비스에서는 서비스 이름으로 바꾼다 */
        const val NAMESPACE = "APP"
        const val DELIMITER = ":"
        const val ESCAPED_DELIMITER = "_"

        /** `@Cacheable` 캐시 키 접두사. 실제 키는 `APP:CACHE:{캐시 이름}:{캐시 키}` */
        const val CACHE_PREFIX = NAMESPACE + DELIMITER + "CACHE" + DELIMITER
    }
}
