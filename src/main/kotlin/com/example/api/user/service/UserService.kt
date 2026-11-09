package com.example.api.user.service

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.domain.repository.MemberRepository
import com.example.api.user.dto.MeResponse
import com.example.api.user.dto.toMeResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 서비스
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val memberRepository: MemberRepository,
) {
    fun getMe(memberId: Long): MeResponse =
        memberRepository.findByIdOrNull(memberId)?.toMeResponse() ?: throw ApiException(ApiStatus.MEMBER_NOT_FOUND)
}
