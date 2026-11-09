package com.example.api.todo.controller

import com.example.api.common.exception.ApiException
import com.example.api.common.response.PageResponse
import com.example.api.common.type.ApiStatus
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoResponse
import com.example.api.todo.dto.TodoSort
import com.example.api.todo.dto.TodoStatus
import com.example.api.todo.service.TodoService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(TodoController::class)
class TodoControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var todoService: TodoService

    @Test
    fun `목록 조회는 쿼리 파라미터를 요청 DTO 로 바인딩하고 페이지 응답을 반환한다`() {
        val request = TodoListRequest(page = 1, size = 5, status = TodoStatus.COMPLETED, sort = TodoSort.TITLE)
        every { todoService.getTodoList(request) } returns
            PageResponse.of(listOf(TodoResponse(1, "스프링 공부", true)), totalElements = 6, page = 1, size = 5)

        mockMvc
            .get("/todos") {
                param("page", "1")
                param("size", "5")
                param("status", "completed")
                param("sort", "title")
            }.andExpect {
                status { isOk() }
                jsonPath("$.statusCode") { value(ApiStatus.OK.code) }
                jsonPath("$.data.content[0].title") { value("스프링 공부") }
                jsonPath("$.data.totalPages") { value(2) }
                jsonPath("$.data.last") { value(true) }
            }
    }

    @Test
    fun `목록 조회 파라미터를 생략하면 기본값으로 조회한다`() {
        every { todoService.getTodoList(TodoListRequest()) } returns PageResponse.of(emptyList(), 0, 0, 10)

        mockMvc.get("/todos").andExpect { status { isOk() } }

        verify { todoService.getTodoList(TodoListRequest()) }
    }

    @Test
    fun `상태 필터와 정렬 값은 대소문자를 구분하지 않는다`() {
        val request = TodoListRequest(status = TodoStatus.INCOMPLETE, sort = TodoSort.ID)
        every { todoService.getTodoList(request) } returns PageResponse.of(emptyList(), 0, 0, 10)

        mockMvc
            .get("/todos") {
                param("status", "INCOMPLETE")
                param("sort", "Id")
            }.andExpect { status { isOk() } }

        verify { todoService.getTodoList(request) }
    }

    @Test
    fun `빈 상태 필터는 기본값으로 조회한다`() {
        every { todoService.getTodoList(TodoListRequest()) } returns PageResponse.of(emptyList(), 0, 0, 10)

        mockMvc.get("/todos") { param("status", "") }.andExpect { status { isOk() } }

        verify { todoService.getTodoList(TodoListRequest()) }
    }

    @Test
    fun `정의되지 않은 상태 필터는 400 과 기본 메시지로 응답한다`() {
        mockMvc.get("/todos") { param("status", "unknown") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.statusCode") { value(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.code) }
            jsonPath("$.message") { value(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.message) }
        }
    }

    @Test
    fun `페이지 크기가 범위를 벗어나면 DTO 에 적은 메시지로 응답한다`() {
        mockMvc.get("/todos") { param("size", "101") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("페이지 크기는 100 이하여야 합니다.") }
        }
    }

    @Test
    fun `단건 조회 대상이 없으면 404 와 TODO_NOT_FOUND 로 응답한다`() {
        every { todoService.getTodo(99) } throws ApiException(ApiStatus.TODO_NOT_FOUND)

        mockMvc.get("/todos/99").andExpect {
            status { isNotFound() }
            jsonPath("$.statusCode") { value(ApiStatus.TODO_NOT_FOUND.code) }
        }
    }

    @Test
    fun `할 일을 생성하면 생성된 할 일을 반환한다`() {
        every { todoService.insertTodo(TodoCreateRequest("스프링 공부")) } returns TodoResponse(1, "스프링 공부", false)

        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title": "스프링 공부"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.completed") { value(false) }
            }
    }

    @Test
    fun `내용 없이 생성하면 400 과 검증 메시지로 응답한다`() {
        mockMvc
            .post("/todos") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title": " "}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("할 일 내용을 입력해주세요.") }
            }
    }

    @Test
    fun `완료 처리하면 완료된 할 일을 반환한다`() {
        every { todoService.updateTodoComplete(1) } returns TodoResponse(1, "스프링 공부", true)

        mockMvc.patch("/todos/1/complete").andExpect {
            status { isOk() }
            jsonPath("$.data.completed") { value(true) }
        }
    }

    @Test
    fun `삭제하면 데이터 없이 성공 응답을 반환한다`() {
        justRun { todoService.deleteTodo(1) }

        mockMvc.delete("/todos/1").andExpect {
            status { isOk() }
            jsonPath("$.statusCode") { value(ApiStatus.OK.code) }
            jsonPath("$.data") { doesNotExist() }
        }

        verify { todoService.deleteTodo(1) }
    }
}
