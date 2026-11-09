package com.example.api.file.service

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.config.aws.S3Properties
import com.example.api.file.dto.PresignedUrlResponse
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration
import java.util.UUID

/**
 * 파일 업로드 [예제: file-upload]
 *
 * 서버는 파일을 받지 않고 S3 에 직접 올릴 수 있는 Presigned URL 만 발급한다.
 * 클라이언트는 presignedUrl 로 PUT 한 뒤 objectUrl 을 저장 API(예: PATCH /todos/{id}/image)에 넘긴다.
 */
@Service
class FileService(
    private val s3Presigner: S3Presigner,
    private val properties: S3Properties,
) {
    fun getPresignedUrl(
        fileName: String,
        contentType: String,
    ): PresignedUrlResponse {
        if (contentType !in ALLOWED_CONTENT_TYPES) throw ApiException(ApiStatus.FILE_CONTENT_TYPE_INVALID)

        // 경로 조작을 막기 위해 파일 이름의 마지막 조각만 쓴다
        val key = "${UUID.randomUUID()}/${fileName.substringAfterLast('/').substringAfterLast('\\')}"
        val presigned =
            s3Presigner.presignPutObject { request ->
                request
                    .signatureDuration(Duration.ofMinutes(properties.presignedUrlExpiryMinutes))
                    .putObjectRequest { it.bucket(properties.bucketName).key(key).contentType(contentType) }
            }
        val url = presigned.url()
        return PresignedUrlResponse(
            presignedUrl = url.toString(),
            objectUrl = "${url.protocol}://${url.authority}${url.path}",
        )
    }

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
    }
}
