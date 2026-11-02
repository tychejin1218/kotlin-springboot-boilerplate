package com.example.api.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer

/**
 * MySQL 컨테이너를 띄우는 통합 테스트 베이스
 *
 * 컨테이너는 JVM 당 한 번만 띄워 모든 하위 테스트가 공유한다. Docker 가 없으면 테스트를 건너뛴다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Suppress("UtilityClassWithPublicConstructor") // 상속용 테스트 베이스라 object 로 바꿀 수 없다
abstract class MySqlContainerTest {
    companion object {
        // lazy: Docker 가 없어 테스트가 건너뛰어질 때는 컨테이너를 만들지 않는다
        private val mysql by lazy {
            MySQLContainer("mysql:8.0")
                .withDatabaseName("boilerplate_db")
                .apply { start() }
        }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("main.datasource.writer-jdbc-url", mysql::getJdbcUrl)
            registry.add("main.datasource.reader-jdbc-url", mysql::getJdbcUrl)
            registry.add("main.datasource.username", mysql::getUsername)
            registry.add("main.datasource.password", mysql::getPassword)
        }
    }
}
