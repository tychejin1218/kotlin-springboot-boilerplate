package com.example.api.todo.service

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.domain.entity.Member
import com.example.api.domain.entity.Todo
import com.example.api.domain.repository.MemberRepository
import com.example.api.domain.repository.TodoRepository
import com.example.api.notification.event.TodoCompleteEvent
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoImageUpdateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoSort
import com.example.api.todo.dto.TodoStatus
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils

class TodoServiceTest {
    private val todoRepository = mockk<TodoRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()
    private val meterRegistry = SimpleMeterRegistry()
    private val todoService = TodoService(todoRepository, memberRepository, eventPublisher, meterRegistry)
    private val member =
        Member(email = EMAIL, name = "홍길동", password = "x").also {
            ReflectionTestUtils.setField(it, "id", 7L)
        }

    @Test
    fun `목록 조회는 회원 이메일과 상태 필터, 정렬을 리포지토리 조건으로 바꿔 넘긴다`() {
        val pageable = slot<Pageable>()
        every { todoRepository.findTodoPage(EMAIL, false, capture(pageable)) } answers {
            PageImpl(listOf(todo(1, "a")), pageable.captured, 1)
        }

        val response =
            todoService.getTodoList(
                EMAIL,
                TodoListRequest(page = 0, size = 10, status = TodoStatus.INCOMPLETE, sort = TodoSort.TITLE),
            )

        assertThat(pageable.captured).isEqualTo(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")))
        assertThat(response.content.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `없거나 다른 회원의 할 일을 조회하면 TODO_NOT_FOUND 예외가 발생한다`() {
        every { todoRepository.findByIdAndMemberEmail(99, EMAIL) } returns null

        assertThatThrownBy { todoService.getTodo(EMAIL, 99) }
            .isInstanceOfSatisfying(ApiException::class.java) {
                assertThat(it.apiStatus).isEqualTo(ApiStatus.TODO_NOT_FOUND)
            }
    }

    @Test
    fun `생성하면 로그인한 회원 소유의 미완료 할 일을 반환한다`() {
        every { memberRepository.findByEmail(EMAIL) } returns member
        every { todoRepository.save(any()) } answers
            { firstArg<Todo>().also { ReflectionTestUtils.setField(it, "id", 1L) } }

        val response = todoService.insertTodo(EMAIL, TodoCreateRequest("스프링 공부"))

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.completed).isFalse()
        assertThat(meterRegistry.counter("todo.create.count").count()).isEqualTo(1.0)
    }

    @Test
    fun `탈퇴한 회원이 생성하면 AUTH_UNAUTHORIZED 예외가 발생한다`() {
        every { memberRepository.findByEmail(EMAIL) } returns null

        assertThatThrownBy { todoService.insertTodo(EMAIL, TodoCreateRequest("스프링 공부")) }
            .isInstanceOfSatisfying(ApiException::class.java) {
                assertThat(it.apiStatus).isEqualTo(ApiStatus.AUTH_UNAUTHORIZED)
            }
    }

    @Test
    fun `완료 처리하면 엔티티 상태가 바뀌고 완료 이벤트를 발행한다`() {
        val todo = todo(1, "스프링 공부")
        every { todoRepository.findByIdAndMemberEmail(1, EMAIL) } returns todo
        justRun { eventPublisher.publishEvent(any<Any>()) }

        val response = todoService.updateTodoComplete(EMAIL, 1)

        assertThat(todo.completed).isTrue()
        assertThat(response.completed).isTrue()
        verify { eventPublisher.publishEvent(TodoCompleteEvent(memberId = 7, todoId = 1, todoTitle = "스프링 공부")) }
    }

    @Test
    fun `이미지를 바꾸면 응답에 이미지 URL 이 담긴다`() {
        every { todoRepository.findByIdAndMemberEmail(1, EMAIL) } returns todo(1, "스프링 공부")

        val response =
            todoService.updateTodoImage(
                EMAIL,
                1,
                TodoImageUpdateRequest("http://localhost:9010/bucket/a.png"),
            )

        assertThat(response.imageUrl).isEqualTo("http://localhost:9010/bucket/a.png")
    }

    @Test
    fun `삭제하면 논리 삭제된다`() {
        val todo = todo(1, "스프링 공부")
        every { todoRepository.findByIdAndMemberEmail(1, EMAIL) } returns todo

        todoService.deleteTodo(EMAIL, 1)

        assertThat(todo.isDeleted).isTrue()
    }

    private fun todo(
        id: Long,
        title: String,
    ) = Todo(title = title, member = member).also { ReflectionTestUtils.setField(it, "id", id) }

    companion object {
        private const val EMAIL = "user1@example.com"
    }
}
