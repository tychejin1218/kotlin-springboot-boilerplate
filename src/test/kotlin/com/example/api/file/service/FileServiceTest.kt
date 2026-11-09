package com.example.api.file.service

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import com.example.api.config.aws.S3Config
import com.example.api.config.aws.S3Properties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class FileServiceTest {
    private val properties =
        S3Properties(
            region = "us-east-1",
            bucketName = "test-bucket",
            endpoint = "http://localhost:9010",
            accessKey = "minioadmin",
            secretKey = "minioadmin",
        )
    private val s3Config = S3Config(properties)

    // Presigned URL 은 로컬에서 서명만 하므로 S3/MinIO 가 없어도 만들 수 있다
    private val fileService = FileService(s3Config.s3Presigner(s3Config.awsCredentialsProvider()), properties)

    @Test
    fun `이미지면 서명된 업로드 URL 과 쿼리 없는 저장 URL 을 돌려준다`() {
        val response = fileService.getPresignedUrl("photo.png", "image/png")

        assertThat(response.presignedUrl).startsWith("http://localhost:9010/test-bucket/").contains("X-Amz-Signature=")
        assertThat(
            response.objectUrl,
        ).startsWith("http://localhost:9010/test-bucket/").endsWith("/photo.png").doesNotContain("?")
    }

    @Test
    fun `파일 이름의 경로는 버리고 마지막 조각만 키에 쓴다`() {
        val response = fileService.getPresignedUrl("../../etc/passwd.png", "image/png")

        assertThat(response.objectUrl).doesNotContain("..").endsWith("/passwd.png")
    }

    @Test
    fun `이미지가 아니면 FILE_CONTENT_TYPE_INVALID`() {
        assertThatThrownBy { fileService.getPresignedUrl("a.html", "text/html") }
            .isInstanceOfSatisfying(ApiException::class.java) {
                assertThat(it.apiStatus).isEqualTo(ApiStatus.FILE_CONTENT_TYPE_INVALID)
            }
    }
}
