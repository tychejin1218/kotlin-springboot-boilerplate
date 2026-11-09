package com.example.api.todo.service

import com.example.api.common.type.RedisKeys
import com.example.api.config.CacheConfig
import com.example.api.domain.entity.Member
import com.example.api.domain.repository.MemberRepository
import com.example.api.support.IntegrationTest
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * 할 일 목록 캐시를 실제 Redis 로 확인한다 (직렬화·역직렬화, 키 형식, 무효화)
 */
class TodoServiceCacheTest(
    @Autowired private val todoService: TodoService,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val redisTemplate: StringRedisTemplate,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : IntegrationTest() {
    private val email = "cache-${UUID.randomUUID()}@test.com"
    private val cachePattern = "${RedisKeys.CACHE_PREFIX}${CacheConfig.TODO_LIST}:*"

    private var memberId = 0L

    @BeforeEach
    fun setUp() {
        redisTemplate.delete(redisTemplate.keys(cachePattern))
        memberId = memberRepository.save(Member(email = email, name = "캐시", password = "x")).requireId()
        todoService.insertTodo(memberId, TodoCreateRequest("캐시할 일"))
    }

    @Test
    fun `목록을 캐시해 두 번째 조회는 DB 를 바꿔도 캐시된 값을 돌려준다`() {
        val first = todoService.getTodoList(memberId, TodoListRequest())
        jdbcTemplate.update("UPDATE todo SET title = '바뀐 제목' WHERE title = '캐시할 일'")

        val second = todoService.getTodoList(memberId, TodoListRequest())

        assertThat(second).isEqualTo(first)
        assertThat(second.content.single().title).isEqualTo("캐시할 일")
    }

    @Test
    fun `캐시 키는 APP CACHE 접두사와 회원 번호로 만들고 이메일을 담지 않는다`() {
        todoService.getTodoList(memberId, TodoListRequest())

        val keys = redisTemplate.keys(cachePattern)
        assertThat(keys).hasSize(1)
        assertThat(keys.single()).doesNotContain(email).endsWith(":$memberId:0:10:ALL:ID")
    }

    @Test
    fun `할 일을 바꾸면 목록 캐시를 비운다`() {
        val before = todoService.getTodoList(memberId, TodoListRequest())

        todoService.updateTodoComplete(memberId, before.content.single().id)

        assertThat(redisTemplate.keys(cachePattern)).isEmpty()
        assertThat(
            todoService
                .getTodoList(memberId, TodoListRequest())
                .content
                .single()
                .completed,
        ).isTrue()
    }
}
