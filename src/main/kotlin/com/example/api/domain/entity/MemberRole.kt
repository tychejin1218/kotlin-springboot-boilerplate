package com.example.api.domain.entity

/** 회원 역할. 이름이 그대로 Spring Security 권한(`hasRole('ADMIN')`)이 된다 */
enum class MemberRole {
    ROLE_USER,
    ROLE_ADMIN,
}
