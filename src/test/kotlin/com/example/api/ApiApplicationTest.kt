package com.example.api

import com.example.api.support.MySqlContainerTest
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

class ApiApplicationTest(
    @Autowired private val dataSource: DataSource,
    @Autowired private val context: ApplicationContext,
) : MySqlContainerTest() {
    @Test
    fun `컨텍스트가 로드되고 기본 DataSource 는 Read-Write 라우팅 프록시다`() {
        assertThat(dataSource).isInstanceOf(LazyConnectionDataSourceProxy::class.java)
    }

    @Test
    fun `Kotlin JDSL 자동 구성이 등록된다`() {
        assertThat(context.getBeanNamesForType(KotlinJdslJpqlExecutor::class.java)).isNotEmpty()
    }
}
