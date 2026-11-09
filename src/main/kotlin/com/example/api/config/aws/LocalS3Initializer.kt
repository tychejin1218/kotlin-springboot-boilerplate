package com.example.api.config.aws

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException

/**
 * 로컬 MinIO 초기화 [예제: file-upload]
 *
 * 시작할 때 버킷을 만들고, 브라우저가 objectUrl 로 이미지를 바로 볼 수 있도록 공개 읽기 정책을 건다.
 * CORS 는 MinIO 서버 전역 설정(기본 "*")을 쓴다. MinIO 는 버킷 단위 CORS API 를 지원하지 않는다.
 */
@Component
@Profile("local")
@ConditionalOnProperty("aws.s3.create-bucket-on-startup", havingValue = "true")
class LocalS3Initializer(
    private val s3Client: S3Client,
    private val properties: S3Properties,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val bucket = properties.bucketName
        try {
            createBucketIfNotExists(bucket)
            s3Client.putBucketPolicy {
                it.bucket(bucket).policy(publicReadPolicy(bucket))
            }
            log.info("MinIO 버킷 준비 완료: {}", bucket)
        } catch (e: SdkException) {
            log.warn("MinIO 초기화 실패 (docker compose up -d minio 로 MinIO 를 띄웠는지 확인하세요): {}", e.message)
        }
    }

    private fun publicReadPolicy(bucket: String) =
        """
        {"Version": "2012-10-17", "Statement": [
          {"Effect": "Allow", "Principal": "*", "Action": "s3:GetObject", "Resource": "arn:aws:s3:::$bucket/*"}
        ]}
        """.trimIndent()

    private fun createBucketIfNotExists(bucket: String) {
        try {
            s3Client.createBucket { it.bucket(bucket) }
        } catch (_: BucketAlreadyOwnedByYouException) {
            log.debug("MinIO 버킷이 이미 있습니다: {}", bucket)
        } catch (_: BucketAlreadyExistsException) {
            log.debug("MinIO 버킷이 이미 있습니다: {}", bucket)
        }
    }
}
