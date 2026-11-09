package com.example.api.config.security

import org.springframework.security.core.AuthenticatedPrincipal

/**
 * 로그인한 회원 (SecurityContext 의 principal)
 *
 * JwtAuthenticationFilter 가 Access 토큰의 회원 번호(mid)와 이메일(sub)로 만든다.
 * 컨트롤러는 `@AuthenticationPrincipal loginMember: LoginMember` 로 받아 [id] 를 서비스에 넘긴다.
 * 회원 번호가 토큰에 있으므로 서비스에서 이메일로 회원을 다시 조회하지 않아도 된다.
 */
data class LoginMember(
    val id: Long,
    val email: String,
) : AuthenticatedPrincipal {
    /** `Authentication.name` 은 이메일이다 */
    override fun getName(): String = email
}
