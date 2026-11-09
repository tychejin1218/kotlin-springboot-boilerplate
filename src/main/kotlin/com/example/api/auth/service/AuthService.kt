package com.example.api.auth.service

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.LoginRequest
import com.example.api.auth.dto.PasswordResetRequest
import com.example.api.auth.dto.RegisterRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.common.component.RedisComponent
import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.common.type.RedisKeys
import com.example.api.domain.entity.Member
import com.example.api.domain.repository.MemberRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/**
 * 회원가입 · 이메일 인증 · 로그인 · 비밀번호 재설정
 *
 * 토큰 발급과 재발급은 [TokenService] 가 맡는다.
 */
@Service
@Transactional(readOnly = true)
class AuthService(
    private val tokenService: TokenService,
    private val redisComponent: RedisComponent,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
) {
    /** 회원가입. 이메일 인증을 마쳐야 로그인할 수 있다 */
    @Transactional
    fun register(request: RegisterRequest) {
        if (memberRepository.existsByEmail(request.email)) {
            throw ApiException(ApiStatus.MEMBER_EMAIL_DUPLICATED)
        }
        memberRepository.save(
            Member(email = request.email, name = request.name, password = encodePassword(request.password)),
        )
        sendVerificationEmail(request.email)
    }

    /** 이메일 인증 후 바로 로그인 상태가 되도록 토큰을 발급한다 */
    @Transactional
    fun verifyEmail(token: String): TokenResponse {
        val key = RedisKeys.EMAIL_VERIFY_TOKEN.key(token)
        val email = redisComponent.getValue(key) ?: throw ApiException(ApiStatus.AUTH_VERIFY_TOKEN_INVALID)
        val member = memberRepository.findByEmail(email) ?: throw ApiException(ApiStatus.MEMBER_NOT_FOUND)
        member.verify()
        redisComponent.delete(key)
        return tokenService.issue(member)
    }

    fun resendVerification(request: EmailRequest) {
        val member = memberRepository.findByEmail(request.email) ?: throw ApiException(ApiStatus.MEMBER_NOT_FOUND)
        if (member.emailVerified) throw ApiException(ApiStatus.AUTH_EMAIL_ALREADY_VERIFIED)
        sendVerificationEmail(member.email)
    }

    /** 이메일이 없을 때와 비밀번호가 틀렸을 때 같은 오류로 응답해 가입 여부를 드러내지 않는다 */
    fun login(request: LoginRequest): TokenResponse {
        val member =
            memberRepository
                .findByEmail(request.email)
                ?.takeIf { passwordEncoder.matches(request.password, it.password) }
                ?: throw ApiException(ApiStatus.AUTH_LOGIN_FAILED)
        if (!member.emailVerified) throw ApiException(ApiStatus.AUTH_EMAIL_NOT_VERIFIED)
        return tokenService.issue(member)
    }

    /** 가입 여부를 드러내지 않도록 회원이 없어도 같은 응답을 한다 */
    fun requestPasswordReset(request: EmailRequest) {
        val member = memberRepository.findByEmail(request.email) ?: return
        val token = UUID.randomUUID().toString()
        redisComponent.setValue(
            RedisKeys.PASSWORD_RESET_TOKEN.key(token),
            member.email,
            ttlOf(RedisKeys.PASSWORD_RESET_TOKEN),
        )
        emailService.sendPasswordResetEmail(member.email, token)
    }

    @Transactional
    fun resetPassword(request: PasswordResetRequest) {
        val key = RedisKeys.PASSWORD_RESET_TOKEN.key(request.token)
        val email = redisComponent.getValue(key) ?: throw ApiException(ApiStatus.AUTH_PASSWORD_RESET_TOKEN_INVALID)
        val member = memberRepository.findByEmail(email) ?: throw ApiException(ApiStatus.MEMBER_NOT_FOUND)
        member.changePassword(encodePassword(request.newPassword))
        redisComponent.delete(key)
    }

    private fun sendVerificationEmail(email: String) {
        val token = UUID.randomUUID().toString()
        redisComponent.setValue(RedisKeys.EMAIL_VERIFY_TOKEN.key(token), email, ttlOf(RedisKeys.EMAIL_VERIFY_TOKEN))
        emailService.sendVerificationEmail(email, token)
    }

    // PasswordEncoder.encode 는 null 입력을 위해 String? 를 돌려준다
    private fun encodePassword(raw: String): String = checkNotNull(passwordEncoder.encode(raw))

    private fun ttlOf(key: RedisKeys): Duration = checkNotNull(key.ttl) { "${key.name} 은 고정 TTL 이 없습니다." }
}
