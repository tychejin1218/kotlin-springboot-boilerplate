package com.example.api.file.controller

import com.example.api.common.response.BaseResponse
import com.example.api.common.response.ErrorResponse
import com.example.api.file.dto.PresignedUrlResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * 파일 API 문서 [예제: file-upload]
 */
@Tag(name = "파일", description = "S3 Presigned URL 발급 API")
interface FileControllerDocs {
    @Operation(
        summary = "업로드용 Presigned URL 발급",
        description = "presignedUrl 로 파일을 PUT 한 뒤 objectUrl 을 저장한다. 이미지(jpeg, png, gif, webp)만 허용한다.",
    )
    @ApiResponse(responseCode = "200", description = "발급 성공")
    @ApiResponse(
        responseCode = "400",
        description = "FILE_CONTENT_TYPE_INVALID",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun getPresignedUrl(
        @Parameter(description = "파일 이름", example = "image.png") fileName: String,
        @Parameter(description = "MIME 타입", example = "image/png") contentType: String,
    ): BaseResponse<PresignedUrlResponse>
}
