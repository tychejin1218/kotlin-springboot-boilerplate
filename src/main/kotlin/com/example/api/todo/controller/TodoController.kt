package com.example.api.todo.controller

import com.example.api.common.response.BaseResponse
import com.example.api.common.response.PageResponse
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoResponse
import com.example.api.todo.service.TodoService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 할 일 API
 */
@RestController
@RequestMapping("/todos")
class TodoController(
    private val todoService: TodoService,
) : TodoControllerDocs {
    @GetMapping
    override fun getTodoList(
        @Valid @ParameterObject @ModelAttribute request: TodoListRequest,
    ): BaseResponse<PageResponse<TodoResponse>> = BaseResponse.ok(todoService.getTodoList(request))

    @GetMapping("/{id}")
    override fun getTodo(
        @PathVariable id: Long,
    ): BaseResponse<TodoResponse> = BaseResponse.ok(todoService.getTodo(id))

    @PostMapping
    override fun insertTodo(
        @Valid @RequestBody request: TodoCreateRequest,
    ): BaseResponse<TodoResponse> = BaseResponse.ok(todoService.insertTodo(request))

    @PatchMapping("/{id}/complete")
    override fun updateTodoComplete(
        @PathVariable id: Long,
    ): BaseResponse<TodoResponse> = BaseResponse.ok(todoService.updateTodoComplete(id))

    @DeleteMapping("/{id}")
    override fun deleteTodo(
        @PathVariable id: Long,
    ): BaseResponse<Unit> {
        todoService.deleteTodo(id)
        return BaseResponse.ok()
    }
}
