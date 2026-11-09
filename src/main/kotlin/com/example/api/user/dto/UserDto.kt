package com.example.api.user.dto

import com.example.api.domain.entity.Member
import io.swagger.v3.oas.annotations.media.Schema

/** 내 정보 응답 */
data class MeResponse(
    @Schema(description = "회원 ID", example = "1")
    val id: Long,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "이메일", example = "user1@example.com")
    val email: String,
)

fun Member.toMeResponse(): MeResponse =
    MeResponse(
        id = checkNotNull(id) { "저장되지 않은 회원은 응답으로 변환할 수 없습니다." },
        name = name,
        email = email,
    )
