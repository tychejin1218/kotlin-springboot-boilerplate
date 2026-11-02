package com.example.api.common.response

import com.example.api.common.type.ApiStatus

/**
 * API 공통 성공 응답
 *
 * 메시지를 바꿀 때는 `BaseResponse.ok(data).copy(message = "...")`
 */
data class BaseResponse<T>(
    val statusCode: String,
    val message: String,
    val data: T? = null,
) {
    companion object {
        fun <T> ok(data: T): BaseResponse<T> = BaseResponse(ApiStatus.OK.code, ApiStatus.OK.message, data)

        fun ok(): BaseResponse<Unit> = BaseResponse(ApiStatus.OK.code, ApiStatus.OK.message)
    }
}
