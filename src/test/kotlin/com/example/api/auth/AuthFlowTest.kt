package com.example.api.auth

import com.example.api.support.IntegrationTest
import com.jayway.jsonpath.JsonPath
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

/**
 * 가입부터 로그아웃까지 실제 MySQL·Redis 로 인증 흐름을 확인한다
 */
@AutoConfigureMockMvc
class AuthFlowTest(
    @Autowired private val mockMvc: MockMvc,
) : IntegrationTest() {
    @Test
    fun `가입하고 메일 링크로 인증하면 바로 로그인 상태가 되고 내 정보를 조회할 수 있다`() {
        val email = uniqueEmail()
        register(email)

        // 인증 전에는 로그인할 수 없다
        postJson("/auth/login", """{"email": "$email", "password": "$PASSWORD"}""").andExpect {
            status { isForbidden() }
            jsonPath("$.statusCode") { value("AUTH_EMAIL_NOT_VERIFIED") }
        }

        val token = slot<String>()
        verify { emailService.sendVerificationEmail(email, capture(token)) }
        val accessToken =
            mockMvc
                .get("/auth/verify-email") { param("token", token.captured) }
                .andExpect { status { isOk() } }
                .read<String>("$.data.accessToken")

        mockMvc
            .get("/users/me") { header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken") }
            .andExpect {
                status { isOk() }
                jsonPath("$.data.email") { value(email) }
            }

        // 인증 링크는 한 번만 쓸 수 있다
        mockMvc.get("/auth/verify-email") { param("token", token.captured) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.statusCode") { value("AUTH_VERIFY_TOKEN_INVALID") }
        }
    }

    @Test
    fun `재발급한 뒤 이전 Refresh 토큰을 다시 쓰면 세션이 끊겨 새 토큰도 쓸 수 없다`() {
        val (_, refreshToken) = login(SEED_EMAIL)

        val newRefreshToken =
            postJson("/auth/token/refresh", refreshBody(refreshToken))
                .andExpect {
                    status { isOk() }
                }.read<String>("$.data.refreshToken")

        postJson("/auth/token/refresh", refreshBody(refreshToken)).andExpect {
            status { isUnauthorized() }
            jsonPath("$.statusCode") { value("AUTH_REFRESH_TOKEN_INVALID") }
        }
        postJson("/auth/token/refresh", refreshBody(newRefreshToken)).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `로그아웃하면 Access 토큰과 Refresh 토큰을 더 쓸 수 없다`() {
        val (accessToken, refreshToken) = login(SEED_EMAIL)

        mockMvc
            .post("/auth/logout") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
                content = refreshBody(refreshToken)
            }.andExpect { status { isOk() } }

        mockMvc.get("/users/me") { header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken") }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.statusCode") { value("AUTH_UNAUTHORIZED") }
        }
        postJson("/auth/token/refresh", refreshBody(refreshToken)).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `비밀번호를 재설정하면 새 비밀번호로만 로그인된다`() {
        val email = uniqueEmail()
        register(email)
        val verifyToken = slot<String>()
        verify { emailService.sendVerificationEmail(email, capture(verifyToken)) }
        mockMvc.get("/auth/verify-email") { param("token", verifyToken.captured) }

        postJson("/auth/password/reset-request", """{"email": "$email"}""").andExpect { status { isOk() } }
        val resetToken = slot<String>()
        verify { emailService.sendPasswordResetEmail(email, capture(resetToken)) }
        postJson("/auth/password/reset", """{"token": "${resetToken.captured}", "newPassword": "NewPass123!"}""")
            .andExpect { status { isOk() } }

        postJson(
            "/auth/login",
            """{"email": "$email", "password": "$PASSWORD"}""",
        ).andExpect { status { isUnauthorized() } }
        postJson("/auth/login", """{"email": "$email", "password": "NewPass123!"}""").andExpect { status { isOk() } }
    }

    @Test
    fun `다른 회원의 할 일은 조회도 변경도 할 수 없다`() {
        val (ownerToken) = login(SEED_EMAIL)
        val (otherToken) = login("user2@example.com")
        val todoId =
            mockMvc
                .post("/todos") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $ownerToken")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"title": "내 할 일"}"""
                }.andExpect { status { isOk() } }
                .read<Int>("$.data.id")

        mockMvc.get("/todos/$todoId") { header(HttpHeaders.AUTHORIZATION, "Bearer $otherToken") }.andExpect {
            status { isNotFound() }
            jsonPath("$.statusCode") { value("TODO_NOT_FOUND") }
        }
        mockMvc
            .get("/todos/$todoId") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $ownerToken")
            }.andExpect { status { isOk() } }
    }

    private fun register(email: String) {
        postJson("/auth/register", """{"email": "$email", "name": "테스트", "password": "$PASSWORD"}""").andExpect {
            status { isOk() }
        }
    }

    /** 시드 회원(비밀번호 Test1234!)으로 로그인해 (accessToken, refreshToken) 을 돌려준다 */
    private fun login(email: String): Pair<String, String> {
        val body =
            postJson("/auth/login", """{"email": "$email", "password": "$PASSWORD"}""")
                .andExpect {
                    status { isOk() }
                }.andReturn()
                .response.contentAsString
        return JsonPath.read<String>(body, "$.data.accessToken") to JsonPath.read(body, "$.data.refreshToken")
    }

    private fun postJson(
        path: String,
        json: String,
    ): ResultActionsDsl =
        mockMvc.post(path) {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }

    private fun refreshBody(refreshToken: String) = """{"refreshToken": "$refreshToken"}"""

    private fun <T> ResultActionsDsl.read(path: String): T = JsonPath.read(andReturn().response.contentAsString, path)

    private fun uniqueEmail() = "flow-${UUID.randomUUID()}@test.com"

    companion object {
        private const val SEED_EMAIL = "user1@example.com"
        private const val PASSWORD = "Test1234!"
    }
}
