package com.example.api.config.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.RedisMessageListenerContainer

/**
 * Redis Pub/Sub 리스너 컨테이너 [예제: notification]
 *
 * 서버가 여러 대여도 Redis 채널을 거쳐 SSE 를 연결한 서버로 알림이 전달된다.
 */
@Configuration(proxyBeanMethods = false)
class RedisPubSubConfig {
    @Bean
    fun redisMessageListenerContainer(connectionFactory: RedisConnectionFactory) =
        RedisMessageListenerContainer().apply { setConnectionFactory(connectionFactory) }
}
