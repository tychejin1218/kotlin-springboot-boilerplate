package com.example.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * 할 일
 *
 * 논리 삭제된 행(deleted_at IS NOT NULL)은 모든 조회에서 제외된다.
 */
@Entity
@Table(name = "todo")
@SQLRestriction("deleted_at IS NULL")
class Todo(
    title: String,
) : BaseAudit() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "title", nullable = false, length = 200)
    var title: String = title
        protected set

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false
        protected set

    /** 완료 처리 */
    fun complete() {
        completed = true
    }
}
