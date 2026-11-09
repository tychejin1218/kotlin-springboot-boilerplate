package com.example.api.config

import com.example.api.common.type.RedisKeys
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.BatchStrategies
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule

/**
 * Redis 캐시 설정 (`@Cacheable`, `@CacheEvict`)
 *
 * - 키: Spring 기본 접두사(`{캐시 이름}::`) 대신 RedisKeys 규칙에 맞춘 `APP:CACHE:{캐시 이름}:{캐시 키}`
 * - 값: 타입 정보를 담은 JSON. 역직렬화할 수 있는 타입은 이 프로젝트 클래스와 표준 컬렉션·기본 타입으로 제한한다
 *   (아무 클래스나 만들 수 있으면 Redis 에 값을 쓸 수 있는 공격자가 임의 코드를 실행할 수 있다)
 * - 기본 TTL 30분, null 은 캐시하지 않는다
 * - 쓰기·삭제는 바로 실행한다(immediateWrites). Spring Data Redis 4 기본값은 비동기라서, 변경 직후 목록을 다시 읽으면
 *   캐시 삭제보다 조회가 먼저 끝나 바뀌기 전 목록이 나올 수 있다
 * - 캐시를 비울 때 KEYS 대신 SCAN 으로 키를 찾는다 (KEYS 는 Redis 전체를 멈춘다)
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
class CacheConfig {
    @Bean
    fun redisCacheCustomizer(connectionFactory: RedisConnectionFactory) =
        RedisCacheManagerBuilderCustomizer { builder ->
            builder.cacheWriter(
                RedisCacheWriter.create(connectionFactory) {
                    it.immediateWrites().batchStrategy(BatchStrategies.scan(SCAN_BATCH_SIZE))
                },
            )
            builder.cacheDefaults(
                RedisCacheConfiguration
                    .defaultCacheConfig()
                    .entryTtl(DEFAULT_TTL)
                    .computePrefixWith { cacheName -> RedisKeys.CACHE_PREFIX + cacheName + RedisKeys.DELIMITER }
                    .serializeValuesWith(SerializationPair.fromSerializer(valueSerializer()))
                    .disableCachingNullValues(),
            )
        }

    private fun valueSerializer(): GenericJacksonJsonRedisSerializer {
        val typeValidator =
            BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType("com.example.api.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("kotlin.")
                .build()
        return GenericJacksonJsonRedisSerializer
            .builder()
            .enableDefaultTyping(typeValidator)
            .customize { it.addModule(KotlinModule.Builder().build()) }
            .build()
    }

    companion object {
        private val DEFAULT_TTL = java.time.Duration.ofMinutes(30)
        private const val SCAN_BATCH_SIZE = 1000

        /** 할 일 목록 캐시 (회원별·조회 조건별) [예제: todo] */
        const val TODO_LIST = "todo:list"
    }
}
