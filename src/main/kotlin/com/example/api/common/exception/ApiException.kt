package com.example.api.common.exception

import com.example.api.common.type.ApiStatus
import org.springframework.http.HttpStatus

/**
 * API 비즈니스 예외
 *
 * HTTP 상태를 생략하면 ApiStatus 에 정의된 기본 상태(NOT_FOUND → 404 등)로 응답한다.
 * ```
 * throw ApiException(ApiStatus.NOT_FOUND)
 * throw ApiException(ApiStatus.CUSTOM_EXCEPTION, message = "상세 메시지")
 * throw ApiException(ApiStatus.INVALID_REQUEST, HttpStatus.UNPROCESSABLE_CONTENT)
 * ```
 */
class ApiException(
    val apiStatus: ApiStatus,
    val httpStatus: HttpStatus = apiStatus.httpStatus,
    message: String = apiStatus.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
