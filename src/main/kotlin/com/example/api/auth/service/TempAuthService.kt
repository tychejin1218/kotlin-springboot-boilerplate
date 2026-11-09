package com.example.api.auth.service

import com.example.api.auth.dto.EmailRequest
import com.example.api.auth.dto.TokenResponse
import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.domain.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 임시 토큰 발급 (local 전용)
 *
 * 비밀번호 없이 이메일만으로 토큰을 발급한다. 로컬 Swagger 테스트용이다.
 * 이메일만 알면 관리자 토큰까지 받을 수 있으므로 외부에서 접근 가능한 dev/stg 에서는 켜지 않는다.
 */
@Service
@Profile("local")
@Transactional(readOnly = true)
class TempAuthService(
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun issueToken(request: EmailRequest): TokenResponse {
        // Refresh 토큰 Redis 키가 회원 번호 기준이라 가입된 회원만 발급한다
        val member = memberRepository.findByEmail(request.email) ?: throw ApiException(ApiStatus.MEMBER_NOT_FOUND)
        log.info("[TempAuth] 임시 토큰 발급: memberId={}", member.id)
        return tokenService.issue(member)
    }
}
