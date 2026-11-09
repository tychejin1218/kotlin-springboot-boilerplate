package com.example.api.todo.dto

import com.example.api.domain.entity.Todo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Sort

/** 할 일 목록 조회 요청 (query string) */
data class TodoListRequest(
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    val page: Int = 0,
    @Schema(description = "페이지 크기 (1~100)", example = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    val size: Int = 10,
    @Schema(
        description = "완료 상태 필터",
        type = "string",
        allowableValues = ["all", "completed", "incomplete"],
        defaultValue = "all",
    )
    val status: TodoStatus = TodoStatus.ALL,
    @Schema(
        description = "정렬 기준 (id: 최신순, title: 제목순)",
        type = "string",
        allowableValues = ["id", "title"],
        defaultValue = "id",
    )
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
    @Schema(description = "할 일 내용", example = "스프링 부트 공부하기")
    @NotBlank(message = "할 일 내용을 입력해주세요.")
    @Size(max = 200, message = "할 일 내용은 200자 이내로 입력해주세요.")
    val title: String,
)

/** 할 일 이미지 URL 변경 요청 [예제: file-upload] */
data class TodoImageUpdateRequest(
    @Schema(
        description = "Presigned URL 로 업로드한 파일의 objectUrl",
        example = "http://localhost:9010/boilerplate-bucket/uuid/image.png",
    )
    @NotBlank(message = "이미지 URL을 입력해주세요.")
    @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다.")
    val imageUrl: String,
)

/** 할 일 응답 */
data class TodoResponse(
    @Schema(description = "할 일 ID", example = "1")
    val id: Long,
    @Schema(description = "할 일 내용", example = "스프링 부트 공부하기")
    val title: String,
    @Schema(description = "완료 여부", example = "false")
    val completed: Boolean,
    @Schema(
        description = "첨부 이미지 URL [예제: file-upload]",
        example = "http://localhost:9010/boilerplate-bucket/uuid/image.png",
    )
    val imageUrl: String? = null,
)

fun Todo.toResponse(): TodoResponse =
    TodoResponse(
        id = requireId(),
        title = title,
        completed = completed,
        imageUrl = imageUrl,
    )
