package com.example.api.common.response

import com.example.api.common.type.ApiStatus
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * API 공통 오류 응답
 */
data class ErrorResponse(
    val statusCode: String,
    val message: String,
    val method: String,
    val path: String,
    val timestamp: String = LocalDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FORMATTER),
) {
    companion object {
        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        fun of(
            apiStatus: ApiStatus,
            method: String,
            path: String,
            message: String = apiStatus.message,
        ): ErrorResponse = ErrorResponse(apiStatus.code, message, method, path)
    }
}
