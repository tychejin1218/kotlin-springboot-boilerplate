package com.example.api.admin.controller

import com.example.api.admin.dto.AdminMemberResponse
import com.example.api.common.response.BaseResponse
import com.example.api.common.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * 관리자 API 문서 [예제: admin]
 */
@Tag(name = "관리자", description = "관리자 전용 API (ROLE_ADMIN)")
interface AdminControllerDocs {
    @Operation(summary = "전체 회원 목록", description = "탈퇴(논리 삭제)한 회원은 제외한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "403",
        description = "PERMISSION_FORBIDDEN",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getMemberList(): BaseResponse<List<AdminMemberResponse>>
}
