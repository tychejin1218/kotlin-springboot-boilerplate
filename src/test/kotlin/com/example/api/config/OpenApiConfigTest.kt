package com.example.api.config

import com.example.api.support.MySqlContainerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class OpenApiConfigTest(
    @Autowired private val mockMvc: MockMvc,
) : MySqlContainerTest() {
    @Test
    fun `local 프로파일에서는 할 일 API 문서를 제공한다`() {
        mockMvc.get("/api-docs/할 일 API").andExpect {
            status { isOk() }
            jsonPath("$.paths['/todos'].get.parameters[?(@.name == 'status')].schema.enum[0]") { value("all") }
            jsonPath("$.paths['/todos/{id}/complete'].patch.summary") { value("할 일 완료 처리") }
        }
    }
}
