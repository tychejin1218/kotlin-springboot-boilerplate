package com.example.api.auth.controller

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.auth.service.TempAuthService
import com.example.api.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 임시 토큰 발급 API (local 전용)
 */
@RestController
@RequestMapping("/auth")
@Profile("local")
@Tag(name = "인증")
class TempAuthController(
    private val tempAuthService: TempAuthService,
) {
    @PostMapping("/token/temp")
    @Operation(
        summary = "임시 토큰 발급 (local 전용)",
        description = "비밀번호 없이 이메일만으로 토큰을 발급한다. 시드 회원: user1@example.com, admin@example.com",
    )
    @SecurityRequirements
    fun issueTempToken(
        @Valid @RequestBody request: EmailRequest,
    ): BaseResponse<TokenResponse> = BaseResponse.ok(tempAuthService.issueToken(request))
}
