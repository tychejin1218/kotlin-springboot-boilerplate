package com.example.api.todo.dto

import com.example.api.domain.entity.Todo
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Sort

/** 할 일 목록 조회 요청 (query string) */
data class TodoListRequest(
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    val page: Int = 0,
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    val size: Int = 10,
    val status: TodoStatus = TodoStatus.ALL,
    val sort: TodoSort = TodoSort.ID,
)

/** 완료 상태 필터 */
enum class TodoStatus(
    val completed: Boolean?,
) {
    ALL(null),
    COMPLETED(true),
    INCOMPLETE(false),
}

/** 정렬 기준 */
enum class TodoSort(
    val sort: Sort,
) {
    ID(Sort.by(Sort.Direction.DESC, "id")),
    TITLE(Sort.by(Sort.Direction.ASC, "title")),
}

/** 할 일 생성 요청 */
data class TodoCreateRequest(
    @NotBlank(message = "할 일 내용을 입력해주세요.")
    @Size(max = 200, message = "할 일 내용은 200자 이내로 입력해주세요.")
    val title: String,
)

/** 할 일 응답 */
data class TodoResponse(
    val id: Long,
    val title: String,
    val completed: Boolean,
)

fun Todo.toResponse(): TodoResponse =
    TodoResponse(
        id = checkNotNull(id) { "저장되지 않은 Todo 는 응답으로 변환할 수 없습니다." },
        title = title,
        completed = completed,
    )
