package com.example.api.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 토큰 응답 */
data class TokenResponse(
    @Schema(description = "액세스 토큰 (Authorization: Bearer {accessToken})")
    val accessToken: String,
    @Schema(description = "리프레시 토큰")
    val refreshToken: String,
)

/** 회원가입 요청 */
data class RegisterRequest(
    @Schema(description = "이메일", example = "user@example.com")
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름을 입력해주세요.")
    val name: String,
    @Schema(description = "비밀번호 (8자 이상)", example = "Test1234!")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val password: String,
)

/** 로그인 요청 */
data class LoginRequest(
    @Schema(description = "이메일", example = "user1@example.com")
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
    @Schema(description = "비밀번호", example = "Test1234!")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String,
)

/** 토큰 재발급 / 로그아웃 요청 */
data class RefreshRequest(
    @Schema(description = "리프레시 토큰")
    @NotBlank(message = "리프레시 토큰을 입력해주세요.")
    val refreshToken: String,
)

/** 이메일만 받는 요청 (인증 메일 재발송, 비밀번호 재설정 링크 요청, 임시 토큰 발급) */
data class EmailRequest(
    @Schema(description = "이메일", example = "user1@example.com")
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
)

/** 비밀번호 재설정 (새 비밀번호 설정) */
data class PasswordResetRequest(
    @Schema(description = "메일로 받은 재설정 토큰")
    @NotBlank(message = "토큰을 입력해주세요.")
    val token: String,
    @Schema(description = "새 비밀번호 (8자 이상)")
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val newPassword: String,
)
