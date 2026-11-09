package com.example.api.user.controller

import com.example.api.common.response.BaseResponse
import com.example.api.common.response.ErrorResponse
import com.example.api.user.dto.MeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication

/**
 * 회원 API 문서
 */
@Tag(name = "회원", description = "회원 정보 API")
interface UserControllerDocs {
    @Operation(summary = "내 정보 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "401",
        description = "AUTH_UNAUTHORIZED",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getMe(
        @Parameter(hidden = true) authentication: Authentication,
    ): BaseResponse<MeResponse>
}
