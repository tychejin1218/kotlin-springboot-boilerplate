package com.example.api.file.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Presigned URL 응답 [예제: file-upload] */
data class PresignedUrlResponse(
    @Schema(
        description = "파일 업로드용 Presigned URL (이 주소로 파일을 PUT 한다)",
        example = "http://localhost:9010/boilerplate-bucket/uuid/image.png?X-Amz-Algorithm=...",
    )
    val presignedUrl: String,
    @Schema(description = "업로드 후 저장할 파일 URL", example = "http://localhost:9010/boilerplate-bucket/uuid/image.png")
    val objectUrl: String,
)
