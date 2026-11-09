package com.example.api.common.type

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RedisKeysTest {
    @Test
    fun `키는 네임스페이스와 경로, 식별자를 콜론으로 잇는다`() {
        assertThat(RedisKeys.REFRESH_TOKEN.key(3)).isEqualTo("APP:AUTH:REFRESH_TOKEN:3")
    }

    @Test
    fun `식별자 안의 콜론은 밑줄로 바꾸고 null 은 빈 문자열로 둔다`() {
        assertThat(RedisKeys.ACCESS_TOKEN_DENY.key("0:0:1", null)).isEqualTo("APP:AUTH:ACCESS_TOKEN_DENY:0_0_1:")
    }

    @Test
    fun `식별자가 없으면 접두사만 돌려준다`() {
        assertThat(RedisKeys.EMAIL_VERIFY_TOKEN.key()).isEqualTo("APP:AUTH:EMAIL_VERIFY")
    }
}
