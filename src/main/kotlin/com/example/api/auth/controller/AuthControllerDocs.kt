package com.example.api.auth.controller

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.LoginRequest
import com.example.api.auth.dto.PasswordResetRequest
import com.example.api.auth.dto.RefreshRequest
import com.example.api.auth.dto.RegisterRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.common.response.BaseResponse
import com.example.api.common.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * 인증 API 문서
 *
 * 오류 응답의 statusCode 는 각 API 설명에 적은 ApiStatus 이름이다.
 */
@Tag(name = "인증", description = "회원가입, 이메일 인증, 로그인, 토큰 재발급, 비밀번호 재설정 API")
@SecurityRequirements // 인증 없이 호출한다
interface AuthControllerDocs {
    @Operation(summary = "회원가입", description = "가입 후 인증 메일을 보낸다. 이메일 인증을 마쳐야 로그인할 수 있다.")
    @ApiResponse(responseCode = "200", description = "가입 성공")
    @ApiResponse(
        responseCode = "409",
        description = "MEMBER_EMAIL_DUPLICATED",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun register(request: RegisterRequest): BaseResponse<Unit>

    @Operation(summary = "이메일 인증", description = "메일 링크의 토큰으로 인증을 마치고 바로 토큰을 발급한다.")
    @ApiResponse(responseCode = "200", description = "인증 성공")
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_VERIFY_TOKEN_INVALID",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun verifyEmail(
        @Parameter(description = "메일로 받은 인증 토큰") token: String,
    ): BaseResponse<TokenResponse>

    @Operation(summary = "인증 메일 재발송")
    @ApiResponse(responseCode = "200", description = "발송 성공")
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_EMAIL_ALREADY_VERIFIED (404: MEMBER_NOT_FOUND)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun resendVerification(request: EmailRequest): BaseResponse<Unit>

    @Operation(summary = "로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(
        responseCode = "401",
        description = "AUTH_LOGIN_FAILED (403: AUTH_EMAIL_NOT_VERIFIED)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun login(request: LoginRequest): BaseResponse<TokenResponse>

    @Operation(
        summary = "로그아웃",
        description = "Refresh 토큰을 무효화하고, Authorization 헤더의 Access 토큰은 만료될 때까지 차단한다. 토큰이 잘못돼도 성공으로 응답한다.",
    )
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    fun logout(
        request: RefreshRequest,
        @Parameter(description = "Bearer {accessToken} (선택)") authorization: String?,
    ): BaseResponse<Unit>

    @Operation(
        summary = "토큰 재발급",
        description = "새 Access/Refresh 토큰을 발급한다. 이전 Refresh 토큰은 더 쓸 수 없고, 다시 쓰면 세션이 끊긴다.",
    )
    @ApiResponse(responseCode = "200", description = "재발급 성공")
    @ApiResponse(
        responseCode = "401",
        description = "AUTH_REFRESH_TOKEN_INVALID",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun refresh(request: RefreshRequest): BaseResponse<TokenResponse>

    @Operation(summary = "비밀번호 재설정 링크 요청", description = "가입 여부를 드러내지 않도록 회원이 없어도 성공으로 응답한다.")
    @ApiResponse(responseCode = "200", description = "요청 성공")
    fun requestPasswordReset(request: EmailRequest): BaseResponse<Unit>

    @Operation(summary = "비밀번호 재설정")
    @ApiResponse(responseCode = "200", description = "재설정 성공")
    @ApiResponse(
        responseCode = "400",
        description = "AUTH_PASSWORD_RESET_TOKEN_INVALID",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun resetPassword(request: PasswordResetRequest): BaseResponse<Unit>
}
