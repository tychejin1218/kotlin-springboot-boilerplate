package com.example.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 감사(Audit) 컬럼 공통 베이스
 *
 * 값은 JPA 가 채우므로 외부에서는 읽기만 가능하다(protected set).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAudit {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    val isDeleted: Boolean
        get() = deletedAt != null

    /** 논리 삭제 */
    fun softDelete() {
        deletedAt = LocalDateTime.now(ZoneOffset.UTC)
    }
}
