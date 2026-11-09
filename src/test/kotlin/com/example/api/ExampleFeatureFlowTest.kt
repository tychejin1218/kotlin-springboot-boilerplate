package com.example.api

import com.example.api.support.IntegrationTest
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Duration

/**
 * 예제 기능(관리자, 파일 업로드, 알림)을 실제 MySQL·Redis 로 확인한다
 */
@AutoConfigureMockMvc
class ExampleFeatureFlowTest(
    @Autowired private val mockMvc: MockMvc,
) : IntegrationTest() {
    @Test
    fun `관리자만 회원 목록을 조회할 수 있다`() {
        mockMvc.get("/admin/members") { bearer(login("admin@example.com")) }.andExpect {
            status { isOk() }
            jsonPath("$.data[?(@.email == 'admin@example.com')].role") { value("ROLE_ADMIN") }
            jsonPath("$.data[?(@.email == 'deleted@example.com')]") { isEmpty() }
        }
        mockMvc.get("/admin/members") { bearer(login("user1@example.com")) }.andExpect {
            status { isForbidden() }
            jsonPath("$.statusCode") { value("PERMISSION_FORBIDDEN") }
        }
    }

    @Test
    fun `Presigned URL 을 받아 업로드한 objectUrl 을 할 일 이미지로 저장한다`() {
        val token = login("user1@example.com")
        val objectUrl =
            JsonPath.read<String>(
                mockMvc
                    .get("/files/presigned-url") {
                        bearer(token)
                        param("fileName", "photo.png")
                        param("contentType", "image/png")
                    }.andExpect { status { isOk() } }
                    .andReturn()
                    .response.contentAsString,
                "$.data.objectUrl",
            )
        val todoId = createTodo(token, "이미지 붙일 할 일")

        mockMvc
            .patch("/todos/$todoId/image") {
                bearer(token)
                contentType = MediaType.APPLICATION_JSON
                content = """{"imageUrl": "$objectUrl"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.imageUrl") { value(objectUrl) }
            }
    }

    @Test
    fun `알림을 구독하면 할 일을 완료했을 때 SSE 로 완료 알림을 받는다`() {
        val token = login("user2@example.com")
        val todoId = createTodo(token, "알림 받을 할 일")
        val subscription =
            mockMvc
                .get("/notifications/subscribe") {
                    bearer(token)
                    accept = MediaType.TEXT_EVENT_STREAM
                }.andExpect { request { asyncStarted() } }
                .andReturn()

        mockMvc.patch("/todos/$todoId/complete") { bearer(token) }.andExpect { status { isOk() } }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(subscription.response.getContentAsString(Charsets.UTF_8))
                .contains("event:connect")
                .contains("event:notification")
                .contains("\"todoId\":$todoId")
                .contains("알림 받을 할 일")
        }
    }

    private fun login(email: String): String =
        JsonPath.read(
            mockMvc
                .post("/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email": "$email", "password": "Test1234!"}"""
                }.andExpect { status { isOk() } }
                .andReturn()
                .response.contentAsString,
            "$.data.accessToken",
        )

    private fun createTodo(
        token: String,
        title: String,
    ): Int =
        JsonPath.read(
            mockMvc
                .post("/todos") {
                    bearer(token)
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"title": "$title"}"""
                }.andReturn()
                .response.contentAsString,
            "$.data.id",
        )

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer(token: String) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
