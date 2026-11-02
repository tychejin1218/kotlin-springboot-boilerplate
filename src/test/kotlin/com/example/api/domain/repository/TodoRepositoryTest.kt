package com.example.api.domain.repository

import com.example.api.domain.entity.Todo
import com.example.api.support.MySqlContainerTest
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

// 테스트마다 롤백되므로 다른 테스트의 데이터와 섞이지 않는다
@Transactional
class TodoRepositoryTest(
    @Autowired private val todoRepository: TodoRepository,
    @Autowired private val entityManager: EntityManager,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : MySqlContainerTest() {
    @Test
    fun `완료 여부로 거르고 Pageable 의 정렬과 페이지를 적용한다`() {
        saveTodos("다", "가", "나", "라")
        todoRepository.save(Todo(title = "완료").apply { complete() })

        val page = todoRepository.findTodoPage(false, PageRequest.of(0, 3, Sort.by("title")))

        assertThat(page.content.map { it.title }).containsExactly("가", "나", "다")
        assertThat(page.totalElements).isEqualTo(4)
        assertThat(page.totalPages).isEqualTo(2)
    }

    @Test
    fun `완료 여부가 null 이면 전체를 조회한다`() {
        saveTodos("a", "b")
        todoRepository.save(Todo(title = "c").apply { complete() })

        val page = todoRepository.findTodoPage(null, PageRequest.of(0, 10))

        assertThat(page.totalElements).isEqualTo(3)
    }

    @Test
    fun `논리 삭제된 할 일은 목록과 단건 조회에서 제외된다`() {
        val (kept, deleted) = saveTodos("남김", "삭제")
        deleted.softDelete()
        entityManager.flush()
        entityManager.clear()

        val page = todoRepository.findTodoPage(null, PageRequest.of(0, 10))

        assertThat(page.content.map { it.id }).containsExactly(kept.id)
        assertThat(todoRepository.findByIdOrNull(checkNotNull(deleted.id))).isNull()
    }

    @Test
    fun `저장하면 감사 컬럼이 채워진다`() {
        val todo = todoRepository.saveAndFlush(Todo(title = "감사"))

        assertThat(todo.createdAt).isNotNull()
        assertThat(todo.updatedAt).isNotNull()
    }

    @Test
    fun `감사 컬럼은 JVM 시간대와 관계없이 UTC 로 저장된다`() {
        val todo = todoRepository.saveAndFlush(Todo(title = "UTC"))

        val diffSeconds =
            jdbcTemplate.queryForObject(
                "SELECT TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) FROM todo WHERE id = ?",
                Long::class.java,
                todo.id,
            )

        assertThat(diffSeconds).isBetween(-60L, 60L)
    }

    private fun saveTodos(vararg titles: String): List<Todo> = todoRepository.saveAll(titles.map { Todo(title = it) })
}
