package com.example.api.common.exception

import com.example.api.common.type.ApiStatus
import org.springframework.http.HttpStatus

/**
 * API 비즈니스 예외
 *
 * HTTP 상태는 ApiStatus 에 묶여 있으므로 지정하지 않는다(TODO_NOT_FOUND → 404 등).
 * ```
 * throw ApiException(ApiStatus.TODO_NOT_FOUND)
 * throw ApiException(ApiStatus.SYSTEM_CUSTOM_EXCEPTION, message = "상세 메시지")
 * ```
 */
class ApiException(
    val apiStatus: ApiStatus,
    message: String = apiStatus.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    val httpStatus: HttpStatus
        get() = apiStatus.httpStatus
}
