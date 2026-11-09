package com.example.api.common.component

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 문자열 연산
 *
 * Redis 장애가 요청 전체 실패로 번지지 않도록 Redis 예외(DataAccessException)는 로그만 남기고
 * 조회 실패는 null, 변경 실패는 false 로 돌려준다. 그 밖의 예외(버그)는 그대로 던진다.
 * 객체 캐시는 `@Cacheable` 을 쓴다.
 */
@Component
class RedisComponent(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun setValue(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        ignoringRedisFailure("setValue", key, Unit) { stringRedisTemplate.opsForValue().set(key, value, ttl) }
    }

    /** 값이 없거나 비어 있거나 Redis 장애면 null */
    fun getValue(key: String): String? =
        ignoringRedisFailure(
            "getValue",
            key,
            null,
        ) { stringRedisTemplate.opsForValue().get(key) }?.takeIf { it.isNotBlank() }

    /**
     * 값이 [expected] 와 같을 때만 [newValue] 로 바꾸고 TTL 을 다시 건다 (compare-and-set)
     *
     * 조회와 교체를 Lua 스크립트 하나로 실행해 원자적으로 처리한다.
     * Refresh 토큰 교체처럼 같은 값으로 동시에 들어온 요청 중 하나만 성공해야 하는 곳에 쓴다.
     */
    fun compareAndSet(
        key: String,
        expected: String,
        newValue: String,
        ttl: Duration,
    ): Boolean =
        ignoringRedisFailure("compareAndSet", key, false) {
            stringRedisTemplate.execute(
                COMPARE_AND_SET_SCRIPT,
                listOf(key),
                expected,
                newValue,
                ttl.toMillis().toString(),
            ) ==
                1L
        }

    fun delete(key: String): Boolean = ignoringRedisFailure("delete", key, false) { stringRedisTemplate.delete(key) }

    /** 정수 카운터 증가. 처음 만들 때만 TTL 을 건다. Redis 장애면 null */
    fun increment(
        key: String,
        ttl: Duration,
    ): Long? =
        ignoringRedisFailure("increment", key, null) {
            val count = stringRedisTemplate.opsForValue().increment(key)
            if (count == 1L) stringRedisTemplate.expire(key, ttl)
            count
        }

    private inline fun <T> ignoringRedisFailure(
        operation: String,
        key: String,
        fallback: T,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: DataAccessException) {
            log.error("[{}] 실패 - key: {}", operation, key, e)
            fallback
        }

    companion object {
        private val COMPARE_AND_SET_SCRIPT =
            RedisScript.of(
                """
                if redis.call('GET', KEYS[1]) == ARGV[1] then
                  redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                  return 1
                end
                return 0
                """.trimIndent(),
                Long::class.java,
            )
    }
}
