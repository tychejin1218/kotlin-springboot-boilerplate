package com.example.api.domain.repository

import com.example.api.domain.entity.Member
import com.example.api.domain.entity.Todo
import com.example.api.support.IntegrationTest
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

// 테스트마다 롤백되므로 다른 테스트의 데이터와 섞이지 않는다
@Transactional
class TodoRepositoryTest(
    @Autowired private val todoRepository: TodoRepository,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val entityManager: EntityManager,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : IntegrationTest() {
    private lateinit var owner: Member
    private lateinit var other: Member

    @BeforeEach
    fun setUp() {
        owner = memberRepository.save(Member(email = "owner@test.com", name = "소유자", password = "x"))
        other = memberRepository.save(Member(email = "other@test.com", name = "다른 회원", password = "x"))
    }

    @Test
    fun `소유 회원과 완료 여부로 거르고 Pageable 의 정렬과 페이지를 적용한다`() {
        saveTodos(owner, "다", "가", "나", "라")
        saveTodos(other, "남의 할 일")
        todoRepository.save(Todo(title = "완료", member = owner).apply { complete() })

        val page = todoRepository.findTodoPage(OWNER_EMAIL, false, PageRequest.of(0, 3, Sort.by("title")))

        assertThat(page.content.map { it.title }).containsExactly("가", "나", "다")
        assertThat(page.totalElements).isEqualTo(4)
        assertThat(page.totalPages).isEqualTo(2)
    }

    @Test
    fun `완료 여부가 null 이면 소유한 할 일 전체를 id 역순으로 조회한다`() {
        val todos = saveTodos(owner, "a", "b")
        todoRepository.save(Todo(title = "c", member = owner).apply { complete() })
        saveTodos(other, "남의 할 일")

        val page =
            todoRepository.findTodoPage(
                OWNER_EMAIL,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(page.totalElements).isEqualTo(3)
        assertThat(page.content.last().id).isEqualTo(todos.first().id)
    }

    @Test
    fun `다른 회원의 할 일은 단건 조회되지 않는다`() {
        val (todo) = saveTodos(other, "남의 할 일")

        assertThat(todoRepository.findByIdAndMemberEmail(checkNotNull(todo.id), OWNER_EMAIL)).isNull()
        assertThat(todoRepository.findByIdAndMemberEmail(checkNotNull(todo.id), "other@test.com")).isNotNull()
    }

    @Test
    fun `논리 삭제된 할 일은 목록과 단건 조회에서 제외된다`() {
        val (kept, deleted) = saveTodos(owner, "남김", "삭제")
        deleted.softDelete()
        entityManager.flush()
        entityManager.clear()

        val page = todoRepository.findTodoPage(OWNER_EMAIL, null, PageRequest.of(0, 10))

        assertThat(page.content.map { it.id }).containsExactly(kept.id)
        assertThat(todoRepository.findByIdAndMemberEmail(checkNotNull(deleted.id), OWNER_EMAIL)).isNull()
    }

    @Test
    fun `저장하면 감사 컬럼이 채워진다`() {
        val todo = todoRepository.saveAndFlush(Todo(title = "감사", member = owner))

        assertThat(todo.createdAt).isNotNull()
        assertThat(todo.updatedAt).isNotNull()
    }

    @Test
    fun `감사 컬럼은 JVM 시간대와 관계없이 UTC 로 저장된다`() {
        val todo = todoRepository.saveAndFlush(Todo(title = "UTC", member = owner))

        val diffSeconds =
            jdbcTemplate.queryForObject(
                "SELECT TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) FROM todo WHERE id = ?",
                Long::class.java,
                todo.id,
            )

        assertThat(diffSeconds).isBetween(-60L, 60L)
    }

    private fun saveTodos(
        member: Member,
        vararg titles: String,
    ): List<Todo> = todoRepository.saveAll(titles.map { Todo(title = it, member = member) })

    companion object {
        private const val OWNER_EMAIL = "owner@test.com"
    }
}
