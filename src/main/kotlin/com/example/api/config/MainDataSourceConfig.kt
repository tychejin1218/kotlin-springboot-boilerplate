package com.example.api.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

/**
 * Writer/Reader DataSource 설정 (`main.datasource.*`)
 *
 * HikariCP 튜닝 값(`main.datasource.hikari.*`)은 두 풀에 똑같이 적용된다.
 */
@ConfigurationProperties("main.datasource")
data class MainDataSourceProperties(
    val writerJdbcUrl: String,
    val readerJdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String = "com.mysql.cj.jdbc.Driver",
)

/**
 * Read/Write 분리 DataSource
 *
 * `@Transactional(readOnly = true)` 는 Reader, 그 외 트랜잭션은 Writer 로 라우팅된다.
 * 라우팅 DataSource 를 @Primary 로 두기만 하고 JPA·MyBatis·트랜잭션 매니저는 Spring Boot 자동 구성을 그대로 쓴다.
 */
@Configuration(proxyBeanMethods = false)
class MainDataSourceConfig {
    @Bean
    @ConfigurationProperties("main.datasource.hikari")
    fun writerDataSource(properties: MainDataSourceProperties): HikariDataSource =
        properties.createHikari(properties.writerJdbcUrl, "writer-pool")

    @Bean
    @ConfigurationProperties("main.datasource.hikari")
    fun readerDataSource(properties: MainDataSourceProperties): HikariDataSource =
        properties.createHikari(properties.readerJdbcUrl, "reader-pool")

    @Bean
    @Primary
    fun dataSource(
        @Qualifier("writerDataSource") writerDataSource: DataSource,
        @Qualifier("readerDataSource") readerDataSource: DataSource,
    ): DataSource =
        LazyConnectionDataSourceProxy(writerDataSource).apply {
            setReadOnlyDataSource(readerDataSource)
        }

    private fun MainDataSourceProperties.createHikari(
        jdbcUrl: String,
        poolName: String,
    ) = HikariDataSource().also {
        it.jdbcUrl = jdbcUrl
        it.username = username
        it.password = password
        it.driverClassName = driverClassName
        it.poolName = poolName
    }
}
