package com.example.api.auth.controller

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.LoginRequest
import com.example.api.auth.dto.PasswordResetRequest
import com.example.api.auth.dto.RefreshRequest
import com.example.api.auth.dto.RegisterRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.auth.service.AuthService
import com.example.api.auth.service.TokenService
import com.example.api.common.response.BaseResponse
import com.example.api.config.security.JwtAuthenticationFilter
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 API
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) : AuthControllerDocs {
    @PostMapping("/register")
    override fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): BaseResponse<Unit> {
        authService.register(request)
        return BaseResponse.ok()
    }

    @GetMapping("/verify-email")
    override fun verifyEmail(
        @RequestParam token: String,
    ): BaseResponse<TokenResponse> = BaseResponse.ok(authService.verifyEmail(token))

    @PostMapping("/resend-verification")
    override fun resendVerification(
        @Valid @RequestBody request: EmailRequest,
    ): BaseResponse<Unit> {
        authService.resendVerification(request)
        return BaseResponse.ok()
    }

    @PostMapping("/login")
    override fun login(
        @Valid @RequestBody request: LoginRequest,
    ): BaseResponse<TokenResponse> = BaseResponse.ok(authService.login(request))

    @PostMapping("/logout")
    override fun logout(
        @Valid @RequestBody request: RefreshRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): BaseResponse<Unit> {
        val accessToken =
            authorization
                ?.takeIf { it.startsWith(JwtAuthenticationFilter.BEARER_PREFIX) }
                ?.removePrefix(JwtAuthenticationFilter.BEARER_PREFIX)
        tokenService.revoke(request.refreshToken, accessToken)
        return BaseResponse.ok()
    }

    @PostMapping("/token/refresh")
    override fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): BaseResponse<TokenResponse> = BaseResponse.ok(tokenService.refresh(request))

    @PostMapping("/password/reset-request")
    override fun requestPasswordReset(
        @Valid @RequestBody request: EmailRequest,
    ): BaseResponse<Unit> {
        authService.requestPasswordReset(request)
        return BaseResponse.ok()
    }

    @PostMapping("/password/reset")
    override fun resetPassword(
        @Valid @RequestBody request: PasswordResetRequest,
    ): BaseResponse<Unit> {
        authService.resetPassword(request)
        return BaseResponse.ok()
    }
}
