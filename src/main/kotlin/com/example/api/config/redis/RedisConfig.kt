package com.example.api.config.redis

import io.lettuce.core.ReadFrom
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redis 설정
 *
 * 연결은 Spring Boot 자동 구성(`spring.data.redis.*`)을 쓴다.
 * - local: 단일 노드 (`spring.data.redis.host/port`)
 * - dev/stg/prd: 클러스터 (`spring.data.redis.cluster.nodes`). 토폴로지 갱신은 `spring.data.redis.lettuce.cluster.refresh.*`
 */
@Configuration(proxyBeanMethods = false)
class RedisConfig {
    /** 클러스터 모드에서는 읽기를 Replica 에 먼저 보낸다 */
    @Bean
    @ConditionalOnProperty("spring.data.redis.cluster.nodes")
    fun replicaPreferredCustomizer() =
        LettuceClientConfigurationBuilderCustomizer {
            it.readFrom(ReadFrom.REPLICA_PREFERRED)
        }
}
