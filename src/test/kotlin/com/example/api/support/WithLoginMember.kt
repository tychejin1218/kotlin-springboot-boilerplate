package com.example.api.support

import com.example.api.config.security.LoginMember
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

/**
 * 컨트롤러 슬라이스 테스트에서 로그인한 회원([LoginMember])으로 요청한다
 *
 * `@WithMockUser` 는 principal 이 Spring 의 User 라서 `@AuthenticationPrincipal loginMember: LoginMember` 에 들어가지 않는다.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithLoginMemberFactory::class)
annotation class WithLoginMember(
    val id: Long = 1,
    val email: String = "user1@example.com",
    val role: String = "ROLE_USER",
)

class WithLoginMemberFactory : WithSecurityContextFactory<WithLoginMember> {
    override fun createSecurityContext(annotation: WithLoginMember): SecurityContext =
        SecurityContextHolder.createEmptyContext().apply {
            authentication =
                UsernamePasswordAuthenticationToken(
                    LoginMember(annotation.id, annotation.email),
                    null,
                    listOf(SimpleGrantedAuthority(annotation.role)),
                )
        }
}
