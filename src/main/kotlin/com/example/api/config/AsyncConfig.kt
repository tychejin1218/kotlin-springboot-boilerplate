package com.example.api.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.EnableAsync

/**
 * 비동기 처리 설정
 *
 * 메일 발송 같은 I/O 작업을 `@Async` 로 별도 스레드에서 실행한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig {
    /**
     * `@Async` 작업은 다른 스레드에서 실행되어 요청 스레드의 MDC(traceId)를 잃는다.
     * Spring Boot 가 이 빈을 기본 비동기 실행기에 자동으로 적용하므로 비동기 작업 로그에도 원래 요청의 traceId 가 남는다.
     */
    @Bean
    fun mdcTaskDecorator() =
        TaskDecorator { runnable ->
            val context = MDC.getCopyOfContextMap()
            Runnable {
                val previous = MDC.getCopyOfContextMap()
                try {
                    MDC.setContextMap(context.orEmpty())
                    runnable.run()
                } finally {
                    MDC.setContextMap(previous.orEmpty())
                }
            }
        }
}
