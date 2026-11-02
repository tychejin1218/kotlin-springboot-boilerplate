package com.example.api.common.response

/**
 * 페이지 응답
 *
 * @property page 0부터 시작하는 페이지 번호
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            totalElements: Long,
            page: Int,
            size: Int,
        ): PageResponse<T> {
            val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 1
            return PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                last = page >= totalPages - 1,
            )
        }
    }
}
