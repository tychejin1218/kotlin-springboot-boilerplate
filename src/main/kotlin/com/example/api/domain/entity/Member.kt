package com.example.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * 회원
 *
 * 논리 삭제된 회원(deleted_at IS NOT NULL)은 모든 조회에서 제외된다.
 *
 * @param password BCrypt 로 인코딩한 비밀번호 (평문 저장 금지)
 */
@Entity
@Table(name = "member")
@SQLRestriction("deleted_at IS NULL")
class Member(
    email: String,
    name: String,
    password: String,
    role: MemberRole = MemberRole.ROLE_USER,
) : BaseAudit() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "email", nullable = false, length = 200)
    var email: String = email
        protected set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(name = "password", nullable = false, length = 200)
    var password: String = password
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: MemberRole = role
        protected set

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false
        protected set

    /** 이메일 인증 완료 */
    fun verify() {
        emailVerified = true
    }

    fun changePassword(encodedPassword: String) {
        password = encodedPassword
    }
}
