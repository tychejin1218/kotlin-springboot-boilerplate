package com.example.api.config

import com.example.api.support.IntegrationTest
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class MainDataSourceConfigTest
    @Autowired
    constructor(
        private val transactionManager: PlatformTransactionManager,
        private val jdbcTemplate: JdbcTemplate,
        @Qualifier("writerDataSource") private val writer: HikariDataSource,
        @Qualifier("readerDataSource") private val reader: HikariDataSource,
    ) : IntegrationTest() {
        @Test
        fun `readOnly 트랜잭션은 Reader 풀의 커넥션을 사용한다`() {
            transaction(readOnly = true) {
                jdbcTemplate.queryForObject("SELECT 1", Int::class.java)

                assertThat(reader.hikariPoolMXBean.activeConnections).isEqualTo(1)
                assertThat(writer.hikariPoolMXBean.activeConnections).isZero()
            }
        }

        @Test
        fun `일반 트랜잭션은 Writer 풀의 커넥션을 사용한다`() {
            transaction(readOnly = false) {
                jdbcTemplate.queryForObject("SELECT 1", Int::class.java)

                assertThat(writer.hikariPoolMXBean.activeConnections).isEqualTo(1)
                assertThat(reader.hikariPoolMXBean.activeConnections).isZero()
            }
        }

        private fun transaction(
            readOnly: Boolean,
            block: () -> Unit,
        ) {
            TransactionTemplate(transactionManager)
                .apply { isReadOnly = readOnly }
                .executeWithoutResult { block() }
        }
    }
