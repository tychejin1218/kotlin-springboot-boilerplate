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
        id = requireId(),
        name = name,
        email = email,
    )
