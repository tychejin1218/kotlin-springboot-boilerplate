package com.example.api.common.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class PageResponseTest {
    @Test
    fun `전체 건수를 페이지 크기로 나눠 올림한 값이 전체 페이지 수가 된다`() {
        val response = PageResponse.of(content = listOf(1, 2, 3), totalElements = 21, page = 0, size = 10)

        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.last).isFalse()
    }

    @Test
    fun `마지막 페이지면 last 가 true 다`() {
        val response = PageResponse.of(content = listOf(1), totalElements = 21, page = 2, size = 10)

        assertThat(response.last).isTrue()
    }

    @Test
    fun `데이터가 없으면 전체 페이지 수는 0 이고 첫 페이지가 마지막이다`() {
        val response = PageResponse.of(content = emptyList<Int>(), totalElements = 0, page = 0, size = 10)

        assertThat(response.totalPages).isZero()
        assertThat(response.last).isTrue()
    }

    @Test
    fun `Spring Data Page 를 변환하면 내용은 변환 함수를 거치고 페이지 정보는 그대로 옮겨진다`() {
        val page = PageImpl(listOf(1, 2), PageRequest.of(1, 2), 5)

        val response = page.toPageResponse { "item-$it" }

        assertThat(response.content).containsExactly("item-1", "item-2")
        assertThat(response.page).isEqualTo(1)
        assertThat(response.size).isEqualTo(2)
        assertThat(response.totalElements).isEqualTo(5)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.last).isFalse()
    }
}
