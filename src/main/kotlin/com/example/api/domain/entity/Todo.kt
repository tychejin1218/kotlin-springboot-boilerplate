package com.example.api.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * 할 일
 *
 * 회원([member])이 소유하며, 논리 삭제된 행(deleted_at IS NOT NULL)은 모든 조회에서 제외된다.
 */
@Entity
@Table(name = "todo")
@SQLRestriction("deleted_at IS NULL")
class Todo(
    title: String,
    member: Member,
) : BaseAudit() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null
        protected set

    @Column(name = "title", nullable = false, length = 200)
    var title: String = title
        protected set

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false
        protected set

    /** 첨부 이미지 URL [예제: file-upload] */
    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member = member
        protected set

    /** 완료 처리 */
    fun complete() {
        completed = true
    }

    /** 첨부 이미지 변경 [예제: file-upload] */
    fun updateImage(url: String) {
        imageUrl = url
    }
}
