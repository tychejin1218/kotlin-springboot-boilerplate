package com.example.api.auth.service

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.LoginRequest
import com.example.api.auth.dto.RegisterRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.common.component.RedisComponent
import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.domain.entity.Member
import com.example.api.domain.repository.MemberRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.ThrowableAssert.ThrowingCallable
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.util.ReflectionTestUtils

class AuthServiceTest {
    private val tokenService = mockk<TokenService>()
    private val redisComponent = mockk<RedisComponent>(relaxed = true)
    private val memberRepository = mockk<MemberRepository>()
    private val passwordEncoder = BCryptPasswordEncoder(4)
    private val emailService = mockk<EmailService>(relaxed = true)
    private val authService = AuthService(tokenService, redisComponent, memberRepository, passwordEncoder, emailService)

    @Test
    fun `이미 가입된 이메일로 가입하면 MEMBER_EMAIL_DUPLICATED`() {
        every { memberRepository.existsByEmail(EMAIL) } returns true

        assertApiStatus(
            ApiStatus.MEMBER_EMAIL_DUPLICATED,
        ) { authService.register(RegisterRequest(EMAIL, "홍길동", PASSWORD)) }
    }

    @Test
    fun `가입하면 비밀번호를 인코딩해 저장하고 인증 메일을 보낸다`() {
        every { memberRepository.existsByEmail(EMAIL) } returns false
        every { memberRepository.save(any()) } answers { firstArg() }

        authService.register(RegisterRequest(EMAIL, "홍길동", PASSWORD))

        verify {
            memberRepository.save(
                match { it.password != PASSWORD && passwordEncoder.matches(PASSWORD, it.password) },
            )
        }
        verify { emailService.sendVerificationEmail(EMAIL, any()) }
    }

    @Test
    fun `없는 이메일과 틀린 비밀번호는 같은 AUTH_LOGIN_FAILED 로 응답한다`() {
        every { memberRepository.findByEmail("none@test.com") } returns null
        every { memberRepository.findByEmail(EMAIL) } returns member(verified = true)

        assertApiStatus(ApiStatus.AUTH_LOGIN_FAILED) { authService.login(LoginRequest("none@test.com", PASSWORD)) }
        assertApiStatus(ApiStatus.AUTH_LOGIN_FAILED) { authService.login(LoginRequest(EMAIL, "wrong-password")) }
    }

    @Test
    fun `이메일 인증 전에는 로그인할 수 없다`() {
        every { memberRepository.findByEmail(EMAIL) } returns member(verified = false)

        assertApiStatus(ApiStatus.AUTH_EMAIL_NOT_VERIFIED) { authService.login(LoginRequest(EMAIL, PASSWORD)) }
    }

    @Test
    fun `로그인에 성공하면 새 세션의 토큰을 발급한다`() {
        val member = member(verified = true)
        every { memberRepository.findByEmail(EMAIL) } returns member
        every { tokenService.issue(member) } returns TokenResponse("access", "refresh")

        assertThat(authService.login(LoginRequest(EMAIL, PASSWORD)).accessToken).isEqualTo("access")
    }

    @Test
    fun `가입하지 않은 이메일로 비밀번호 재설정을 요청해도 성공으로 끝나고 메일은 보내지 않는다`() {
        every { memberRepository.findByEmail("none@test.com") } returns null

        authService.requestPasswordReset(EmailRequest("none@test.com"))

        verify(exactly = 0) { emailService.sendPasswordResetEmail(any(), any()) }
    }

    private fun member(verified: Boolean) =
        Member(email = EMAIL, name = "홍길동", password = checkNotNull(passwordEncoder.encode(PASSWORD))).also {
            ReflectionTestUtils.setField(it, "id", 1L)
            if (verified) it.verify()
        }

    private fun assertApiStatus(
        expected: ApiStatus,
        call: ThrowingCallable,
    ) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException::class.java) {
            assertThat(it.apiStatus).isEqualTo(expected)
        }
    }

    companion object {
        private const val EMAIL = "user@test.com"
        private const val PASSWORD = "Test1234!"
    }
}
