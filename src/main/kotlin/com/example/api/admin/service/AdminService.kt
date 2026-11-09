package com.example.api.admin.service

import com.example.api.admin.dto.AdminMemberResponse
import com.example.api.admin.dto.toAdminResponse
import com.example.api.domain.repository.MemberRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 서비스 [예제: admin]
 */
@Service
@Transactional(readOnly = true)
class AdminService(
    private val memberRepository: MemberRepository,
) {
    fun getMemberList(): List<AdminMemberResponse> =
        memberRepository.findAll(Sort.by("id")).map { it.toAdminResponse() }
}
