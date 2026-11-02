package com.example.api.todo.service

import com.example.api.common.exception.ApiException
import com.example.api.common.response.PageResponse
import com.example.api.common.response.toPageResponse
import com.example.api.common.type.ApiStatus
import com.example.api.domain.entity.Todo
import com.example.api.domain.repository.TodoRepository
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoResponse
import com.example.api.todo.dto.toResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 할 일 서비스
 *
 * 클래스 기본은 읽기 전용(Reader DB), 변경 메서드만 @Transactional(Writer DB)
 */
@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
) {
    fun getTodoList(request: TodoListRequest): PageResponse<TodoResponse> =
        todoRepository
            .findTodoPage(
                completed = request.status.completed,
                pageable = PageRequest.of(request.page, request.size, request.sort.sort),
            ).toPageResponse { it.toResponse() }

    fun getTodo(id: Long): TodoResponse = findTodo(id).toResponse()

    @Transactional
    fun insertTodo(request: TodoCreateRequest): TodoResponse =
        todoRepository.save(Todo(title = request.title)).toResponse()

    @Transactional
    fun updateTodoComplete(id: Long): TodoResponse {
        val todo = findTodo(id)
        todo.complete()
        return todo.toResponse()
    }

    @Transactional
    fun deleteTodo(id: Long) {
        findTodo(id).softDelete()
    }

    private fun findTodo(id: Long): Todo = todoRepository.findByIdOrNull(id) ?: throw ApiException(ApiStatus.NOT_FOUND)
}
