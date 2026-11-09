package com.example.api.common.advice

import com.example.api.common.exception.ApiException
import com.example.api.common.type.ApiStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

class ExceptionAdviceTest {
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(TestController())
            .setControllerAdvice(ExceptionAdvice())
            .setValidator(LocalValidatorFactoryBean().apply { afterPropertiesSet() })
            .build()

    @Test
    fun `ApiException 에 메시지를 지정하면 HTTP 상태는 그대로 두고 메시지만 바꾼다`() {
        mockMvc.get("/test/api-exception-message").andExpect {
            status { isBadRequest() }
            jsonPath("$.statusCode") { value("SYSTEM_CUSTOM_EXCEPTION") }
            jsonPath("$.message") { value("상세 메시지") }
        }
    }

    @Test
    fun `ApiException 은 ApiStatus 의 HTTP 상태와 enum 이름 응답 코드로 변환된다`() {
        mockMvc.get("/test/api-exception").andExpect {
            status { isNotFound() }
            jsonPath("$.statusCode") { value("RESOURCE_NOT_FOUND") }
            jsonPath("$.message") { value(ApiStatus.RESOURCE_NOT_FOUND.message) }
            jsonPath("$.method") { value("GET") }
            jsonPath("$.path") { value("/test/api-exception") }
            jsonPath("$.timestamp") { isNotEmpty() }
        }
    }

    @Test
    fun `요청 본문 검증 실패 시 DTO 에 적은 메시지를 그대로 내려준다`() {
        mockMvc
            .post("/test/validation") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"title": ""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.statusCode") { value(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.code) }
                jsonPath("$.message") { value("제목을 입력해주세요.") }
            }
    }

    @Test
    fun `읽을 수 없는 요청 본문은 VALIDATION_MESSAGE_NOT_READABLE 로 응답한다`() {
        mockMvc
            .post("/test/validation") {
                contentType = MediaType.APPLICATION_JSON
                content = "{broken"
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.statusCode") { value(ApiStatus.VALIDATION_MESSAGE_NOT_READABLE.code) }
            }
    }

    @Test
    fun `필수 파라미터 누락은 VALIDATION_PARAMETER_MISSING 으로 응답한다`() {
        mockMvc.get("/test/param").andExpect {
            status { isBadRequest() }
            jsonPath("$.statusCode") { value(ApiStatus.VALIDATION_PARAMETER_MISSING.code) }
        }
    }

    @Test
    fun `경로 변수 타입 불일치는 VALIDATION_PARAMETER_TYPE_MISMATCH 로 응답한다`() {
        mockMvc.get("/test/type/abc").andExpect {
            status { isBadRequest() }
            jsonPath("$.statusCode") { value(ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH.code) }
        }
    }

    @Test
    fun `지원하지 않는 HTTP 메서드는 405 와 SYSTEM_METHOD_NOT_SUPPORTED 로 응답한다`() {
        mockMvc.post("/test/param").andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.statusCode") { value(ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED.code) }
        }
    }

    @Test
    fun `지원하지 않는 미디어 타입은 415 와 SYSTEM_MEDIA_TYPE_NOT_SUPPORTED 로 응답한다`() {
        mockMvc
            .post("/test/validation") {
                contentType = MediaType.TEXT_PLAIN
                content = "title"
            }.andExpect {
                status { isUnsupportedMediaType() }
                jsonPath("$.statusCode") { value(ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED.code) }
            }
    }

    @Test
    fun `처리하지 않은 예외는 500 과 SYSTEM_INTERNAL_ERROR 로 응답하고 내부 메시지를 노출하지 않는다`() {
        mockMvc.get("/test/unexpected").andExpect {
            status { isInternalServerError() }
            jsonPath("$.statusCode") { value(ApiStatus.SYSTEM_INTERNAL_ERROR.code) }
            jsonPath("$.message") { value(ApiStatus.SYSTEM_INTERNAL_ERROR.message) }
        }
    }

    data class TestRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        val title: String,
    )

    // 테스트 클래스의 중첩 클래스는 TestTypeExcludeFilter 가 컴포넌트 스캔에서 제외한다
    @RestController
    class TestController {
        @GetMapping("/test/api-exception")
        fun apiException(): Unit = throw ApiException(ApiStatus.RESOURCE_NOT_FOUND)

        @GetMapping("/test/api-exception-message")
        fun apiExceptionMessage(): Unit = throw ApiException(ApiStatus.SYSTEM_CUSTOM_EXCEPTION, message = "상세 메시지")

        @PostMapping("/test/validation")
        fun validation(
            @Valid @RequestBody request: TestRequest,
        ) = request

        @GetMapping("/test/param")
        fun param(
            @RequestParam name: String,
        ) = name

        @GetMapping("/test/type/{id}")
        fun type(
            @PathVariable id: Long,
        ) = id

        @GetMapping("/test/unexpected")
        fun unexpected(): Unit = error("내부 구현 정보")
    }
}
