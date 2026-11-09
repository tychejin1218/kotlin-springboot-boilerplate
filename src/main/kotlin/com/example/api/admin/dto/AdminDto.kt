package com.example.api.admin.dto

import com.example.api.domain.entity.Member
import io.swagger.v3.oas.annotations.media.Schema

/** 회원 응답 (관리자) [예제: admin] */
data class AdminMemberResponse(
    @Schema(description = "회원 ID", example = "1")
    val id: Long,
    @Schema(description = "이메일", example = "user1@example.com")
    val email: String,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "역할 (ROLE_USER, ROLE_ADMIN)", example = "ROLE_USER")
    val role: String,
)

fun Member.toAdminResponse(): AdminMemberResponse =
    AdminMemberResponse(
        id = checkNotNull(id) { "저장되지 않은 회원은 응답으로 변환할 수 없습니다." },
        email = email,
        name = name,
        role = role.name,
    )
