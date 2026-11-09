package com.example.api.support

import com.example.api.auth.service.EmailService
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.MountableFile

/**
 * MySQL·Redis 컨테이너를 띄우는 통합 테스트 베이스
 *
 * 컨테이너는 JVM 당 한 번만 띄워 모든 하위 테스트가 공유한다. Docker 가 없으면 테스트를 건너뛴다.
 * 스키마는 docker compose 와 같은 docker/init.sql 로 만든다.
 * 메일은 실제로 보내지 않도록 [emailService] 를 목으로 바꾼다. 모든 하위 테스트가 같은 목을 써야 스프링 컨텍스트를 재사용한다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegrationTest {
    @MockkBean(relaxed = true)
    protected lateinit var emailService: EmailService

    companion object {
        // lazy: Docker 가 없어 테스트가 건너뛰어질 때는 컨테이너를 만들지 않는다
        private val mysql by lazy {
            MySQLContainer("mysql:8.0")
                .withDatabaseName("boilerplate_db")
                .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql",
                ).apply { start() }
        }

        private val redis by lazy {
            GenericContainer("redis:7.0").withExposedPorts(REDIS_PORT).apply { start() }
        }

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(registry: DynamicPropertyRegistry) {
            // 프로파일별 datasource-*.yml 과 같은 접속 옵션을 붙인다
            val jdbcUrl = { "${mysql.jdbcUrl}?serverTimezone=UTC&characterEncoding=UTF-8" }
            registry.add("main.datasource.writer-jdbc-url", jdbcUrl)
            registry.add("main.datasource.reader-jdbc-url", jdbcUrl)
            registry.add("main.datasource.username", mysql::getUsername)
            registry.add("main.datasource.password", mysql::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(REDIS_PORT) }
        }

        private const val REDIS_PORT = 6379
    }
}
