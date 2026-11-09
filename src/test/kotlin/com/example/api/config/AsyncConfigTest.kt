package com.example.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AsyncConfigTest {
    private val decorator = AsyncConfig().mdcTaskDecorator()

    @AfterEach
    fun tearDown() = MDC.clear()

    @Test
    fun `비동기 작업에 요청 스레드의 MDC 를 복사하고 끝나면 작업 스레드의 MDC 를 되돌린다`() {
        val executor = Executors.newSingleThreadExecutor()
        var traceIdInTask: String? = null
        var mdcAfterTask: Map<String, String>? = null

        MDC.put("traceId", "abc123")
        val task = decorator.decorate { traceIdInTask = MDC.get("traceId") }
        executor.submit(task).get(1, TimeUnit.SECONDS)
        executor.submit { mdcAfterTask = MDC.getCopyOfContextMap().orEmpty() }.get(1, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(traceIdInTask).isEqualTo("abc123")
        assertThat(mdcAfterTask).isEmpty()
    }
}
