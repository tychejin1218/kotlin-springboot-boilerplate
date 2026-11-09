package com.example.api.common.controller

import com.example.api.common.type.ApiStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(HealthController::class)
class HealthControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `헬스 체크는 200 과 성공 코드를 반환한다`() {
        mockMvc.get("/health").andExpect {
            status { isOk() }
            jsonPath("$.statusCode") { value("OK") }
            jsonPath("$.message") { value(ApiStatus.OK.message) }
        }
    }
}
