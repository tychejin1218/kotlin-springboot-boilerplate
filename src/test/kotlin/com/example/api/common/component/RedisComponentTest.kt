package com.example.api.common.component

import com.example.api.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.UUID

class RedisComponentTest(
    @Autowired private val redisComponent: RedisComponent,
) : IntegrationTest() {
    private val key = "TEST:${UUID.randomUUID()}"

    @Test
    fun `compareAndSet 은 현재 값이 예상과 같을 때만 바꾼다`() {
        redisComponent.setValue(key, "old", Duration.ofMinutes(1))

        assertThat(redisComponent.compareAndSet(key, "other", "new", Duration.ofMinutes(1))).isFalse()
        assertThat(redisComponent.compareAndSet(key, "old", "new", Duration.ofMinutes(1))).isTrue()
        assertThat(redisComponent.getValue(key)).isEqualTo("new")
    }

    @Test
    fun `increment 는 처음 만들 때 TTL 을 걸고 1 부터 센다`() {
        assertThat(redisComponent.increment(key, Duration.ofMinutes(1))).isEqualTo(1L)
        assertThat(redisComponent.increment(key, Duration.ofMinutes(1))).isEqualTo(2L)
        redisComponent.delete(key)
        assertThat(redisComponent.getValue(key)).isNull()
    }
}
