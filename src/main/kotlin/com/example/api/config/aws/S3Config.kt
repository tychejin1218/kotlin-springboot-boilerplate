package com.example.api.config.aws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * S3 설정 (`aws.s3.*`) [예제: file-upload]
 *
 * @property endpoint 로컬 MinIO 주소. 비워 두면 AWS S3 에 붙는다
 * @property accessKey [endpoint] 를 쓸 때만 사용하는 정적 자격증명
 * @property createBucketOnStartup local 에서 시작할 때 버킷을 만들고 공개 읽기 정책을 건다 (LocalS3Initializer)
 */
@ConfigurationProperties("aws.s3")
data class S3Properties(
    val region: String,
    val bucketName: String,
    val presignedUrlExpiryMinutes: Long = 5,
    val endpoint: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null,
    val createBucketOnStartup: Boolean = false,
) {
    val hasEndpointOverride: Boolean
        get() = !endpoint.isNullOrBlank()
}

/**
 * AWS S3 클라이언트 [예제: file-upload]
 *
 * endpoint 가 있으면(로컬 MinIO) 정적 자격증명과 path-style 주소를 쓴다.
 * 없으면 DefaultCredentialsProvider 체인(환경변수, ~/.aws, ECS/EKS/EC2 IAM Role 순)으로 자격증명을 찾으므로
 * IAM Role 을 붙인 환경에서는 따로 설정할 것이 없다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(S3Properties::class)
class S3Config(
    private val properties: S3Properties,
) {
    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider =
        if (properties.hasEndpointOverride) {
            StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKey, properties.secretKey))
        } else {
            DefaultCredentialsProvider.builder().build()
        }

    @Bean
    fun s3Client(credentialsProvider: AwsCredentialsProvider): S3Client =
        S3Client
            .builder()
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider)
            .apply {
                if (properties.hasEndpointOverride) {
                    endpointOverride(URI.create(properties.endpoint))
                    serviceConfiguration(PATH_STYLE)
                }
            }.build()

    @Bean
    fun s3Presigner(credentialsProvider: AwsCredentialsProvider): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider)
            .apply {
                if (properties.hasEndpointOverride) {
                    endpointOverride(URI.create(properties.endpoint))
                    serviceConfiguration(PATH_STYLE)
                }
            }.build()

    companion object {
        private val PATH_STYLE: S3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build()
    }
}
