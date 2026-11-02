package com.example.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

/**
 * JPA Auditing 설정
 *
 * ApiApplication 에 두면 @WebMvcTest 슬라이스까지 JPA 를 요구하므로 별도 설정으로 분리한다.
 * created_at/updated_at 도 soft delete(deleted_at)와 같은 UTC 로 기록한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
class JpaConfig {
    @Bean
    fun utcDateTimeProvider() = DateTimeProvider { Optional.of(LocalDateTime.now(ZoneOffset.UTC)) }
}
