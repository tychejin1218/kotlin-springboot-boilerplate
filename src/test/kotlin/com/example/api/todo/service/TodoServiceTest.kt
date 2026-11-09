package com.example.api.todo.service

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.domain.entity.Todo
import com.example.api.domain.repository.TodoRepository
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoSort
import com.example.api.todo.dto.TodoStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class TodoServiceTest {
    private val todoRepository = mockk<TodoRepository>()
    private val todoService = TodoService(todoRepository)

    @Test
    fun `목록 조회는 상태 필터와 정렬을 리포지토리 조건으로 바꿔 넘긴다`() {
        val pageable = slot<Pageable>()
        every { todoRepository.findTodoPage(false, capture(pageable)) } answers {
            PageImpl(listOf(todo(1, "a")), pageable.captured, 1)
        }

        val response =
            todoService.getTodoList(
                TodoListRequest(page = 0, size = 10, status = TodoStatus.INCOMPLETE, sort = TodoSort.TITLE),
            )

        assertThat(pageable.captured).isEqualTo(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")))
        assertThat(response.content.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `없는 할 일을 조회하면 NOT_FOUND 예외가 발생한다`() {
        every { todoRepository.findByIdOrNull(99) } returns null

        assertThatThrownBy { todoService.getTodo(99) }
            .isInstanceOfSatisfying(ApiException::class.java) {
                assertThat(it.apiStatus).isEqualTo(ApiStatus.TODO_NOT_FOUND)
            }
    }

    @Test
    fun `생성하면 저장된 할 일을 미완료 상태로 반환한다`() {
        every { todoRepository.save(any()) } answers
            { firstArg<Todo>().also { ReflectionTestUtils.setField(it, "id", 1L) } }

        val response = todoService.insertTodo(TodoCreateRequest("스프링 공부"))

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.title).isEqualTo("스프링 공부")
        assertThat(response.completed).isFalse()
    }

    @Test
    fun `완료 처리하면 엔티티 상태가 바뀐다`() {
        val todo = todo(1, "스프링 공부")
        every { todoRepository.findById(1) } returns Optional.of(todo)

        val response = todoService.updateTodoComplete(1)

        assertThat(todo.completed).isTrue()
        assertThat(response.completed).isTrue()
    }

    @Test
    fun `삭제하면 논리 삭제된다`() {
        val todo = todo(1, "스프링 공부")
        every { todoRepository.findById(1) } returns Optional.of(todo)

        todoService.deleteTodo(1)

        assertThat(todo.isDeleted).isTrue()
    }

    private fun todo(
        id: Long,
        title: String,
    ) = Todo(title = title).also { ReflectionTestUtils.setField(it, "id", id) }
}
