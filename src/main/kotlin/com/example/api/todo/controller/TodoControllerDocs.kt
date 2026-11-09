package com.example.api.todo.controller

import com.example.api.common.response.BaseResponse
import com.example.api.common.response.ErrorResponse
import com.example.api.common.response.PageResponse
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication

/**
 * 할 일 API 문서 (Swagger 어노테이션)
 *
 * 문서 어노테이션은 이 인터페이스에 모아 두고, 컨트롤러는 매핑과 위임만 남긴다.
 */
@Tag(name = "할 일", description = "할 일 목록 조회/추가/완료/삭제 API")
interface TodoControllerDocs {
    @Operation(summary = "할 일 목록 조회", description = "로그인한 회원의 할 일 목록을 페이지 단위로 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [
            Content(
                examples = [
                    ExampleObject(
                        value = """
                        {
                          "statusCode": "OK",
                          "message": "성공",
                          "data": {
                            "content": [
                              {"id": 2, "title": "리액트 공부하기", "completed": false},
                              {"id": 1, "title": "스프링 부트 공부하기", "completed": true}
                            ],
                            "page": 0,
                            "size": 10,
                            "totalElements": 2,
                            "totalPages": 1,
                            "last": true
                          }
                        }
                        """,
                    ),
                ],
            ),
        ],
    )
    @ApiResponse(
        responseCode = "400",
        description = "파라미터 오류",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getTodoList(
        @Parameter(hidden = true) authentication: Authentication,
        request: TodoListRequest,
    ): BaseResponse<PageResponse<TodoResponse>>

    @Operation(summary = "할 일 단건 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(
        responseCode = "404",
        description = "TODO_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getTodo(
        @Parameter(hidden = true) authentication: Authentication,
        id: Long,
    ): BaseResponse<TodoResponse>

    @Operation(summary = "할 일 추가")
    @ApiResponse(responseCode = "200", description = "추가 성공")
    @ApiResponse(
        responseCode = "400",
        description = "VALIDATION_METHOD_ARGUMENT_INVALID (message 에 검증 메시지)",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun insertTodo(
        @Parameter(hidden = true) authentication: Authentication,
        request: TodoCreateRequest,
    ): BaseResponse<TodoResponse>

    @Operation(summary = "할 일 완료 처리", description = "할 일을 완료 상태로 바꿉니다.")
    @ApiResponse(responseCode = "200", description = "완료 처리 성공")
    @ApiResponse(
        responseCode = "404",
        description = "TODO_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun updateTodoComplete(
        @Parameter(hidden = true) authentication: Authentication,
        id: Long,
    ): BaseResponse<TodoResponse>

    @Operation(summary = "할 일 삭제", description = "할 일을 논리 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiResponse(
        responseCode = "404",
        description = "TODO_NOT_FOUND",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun deleteTodo(
        @Parameter(hidden = true) authentication: Authentication,
        id: Long,
    ): BaseResponse<Unit>
}
