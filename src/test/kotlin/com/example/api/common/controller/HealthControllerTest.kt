package com.example.api.common.controller

import com.example.api.common.type.ApiStatus
import com.example.api.config.web.TraceIdFilter
import com.example.api.support.ImportSecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

@WebMvcTest(HealthController::class)
@ImportSecurityConfig
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

    @Test
    fun `모든 응답에 추적 ID 헤더가 붙는다`() {
        mockMvc.get("/health").andExpect {
            header { exists(TraceIdFilter.HEADER) }
        }
    }

    @Test
    fun `허용한 오리진의 CORS 사전 요청에 허용 헤더로 응답한다`() {
        mockMvc
            .options("/health") {
                header(HttpHeaders.ORIGIN, "http://localhost:5173")
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            }.andExpect {
                status { isOk() }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173") }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") }
            }
    }

    @Test
    fun `허용하지 않은 오리진의 CORS 사전 요청은 거부한다`() {
        mockMvc
            .options("/health") {
                header(HttpHeaders.ORIGIN, "http://evil.example.com")
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            }.andExpect {
                status { isForbidden() }
            }
    }
}
