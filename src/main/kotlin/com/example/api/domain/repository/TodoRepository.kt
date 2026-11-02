package com.example.api.domain.repository

import com.example.api.domain.entity.Todo
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TodoRepository :
    JpaRepository<Todo, Long>,
    KotlinJdslJpqlExecutor {
    /**
     * 할 일 목록 페이지 조회
     *
     * @param completed 완료 여부 필터 (null 이면 전체)
     * @param pageable 페이지·정렬 (정렬은 Kotlin JDSL 이 JPQL 에 붙인다)
     */
    fun findTodoPage(
        completed: Boolean?,
        pageable: Pageable,
    ): Page<Todo> =
        findPage(pageable) {
            select(entity(Todo::class))
                .from(entity(Todo::class))
                .whereAnd(
                    completed?.let { path(Todo::completed).eq(it) },
                )
        }
}
