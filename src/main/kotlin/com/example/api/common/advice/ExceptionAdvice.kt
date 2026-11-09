package com.example.api.common.advice

import com.example.api.common.exception.ApiException
import com.example.api.common.response.ErrorResponse
import com.example.api.common.type.ApiStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 전역 예외 처리
 */
@RestControllerAdvice
class ExceptionAdvice {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        if (ex.httpStatus.is5xxServerError) {
            log.error("ApiException: {} {}", request.method, request.requestURI, ex)
        } else {
            log.warn("ApiException: {} {} - {}", request.method, request.requestURI, ex.message)
        }
        return respond(ex.apiStatus, request, ex.message)
    }

    /**
     * 클라이언트 요청 오류(4xx) 처리. 스택 트레이스 없이 warn 으로만 남긴다
     */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
        ConstraintViolationException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
        NoHandlerFoundException::class,
        NoResourceFoundException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpMediaTypeNotSupportedException::class,
    )
    fun handleClientError(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val apiStatus =
            when (ex) {
                is MethodArgumentNotValidException -> ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID
                is HandlerMethodValidationException -> ApiStatus.VALIDATION_CONSTRAINT_VIOLATED
                is ConstraintViolationException -> ApiStatus.VALIDATION_CONSTRAINT_VIOLATED
                is MissingServletRequestParameterException -> ApiStatus.VALIDATION_PARAMETER_MISSING
                is MethodArgumentTypeMismatchException -> ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH
                is NoHandlerFoundException -> ApiStatus.SYSTEM_NO_HANDLER_FOUND
                is NoResourceFoundException -> ApiStatus.SYSTEM_NO_HANDLER_FOUND
                is HttpRequestMethodNotSupportedException -> ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED
                is HttpMediaTypeNotSupportedException -> ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED
                else -> ApiStatus.VALIDATION_MESSAGE_NOT_READABLE
            }
        log.warn("{}: {} {} - {}", ex.javaClass.simpleName, request.method, request.requestURI, ex.message)
        return respond(apiStatus, request, validationMessage(ex))
    }

    /**
     * 메서드 수준 권한 거부(`@PreAuthorize`). URL 수준 거부는 SecurityConfig 의 accessDeniedHandler 가 처리하지만
     * `@PreAuthorize` 는 컨트롤러 메서드를 호출할 때 검사해서 예외가 여기로 온다. 처리하지 않으면 500 이 된다
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("AccessDeniedException: {} {}", request.method, request.requestURI)
        return respond(ApiStatus.PERMISSION_FORBIDDEN, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Exception: {} {}", request.method, request.requestURI, ex)
        return respond(ApiStatus.SYSTEM_INTERNAL_ERROR, request)
    }

    /**
     * DTO 에 적은 검증 메시지(@NotBlank(message = ...))를 응답에 그대로 노출한다.
     * 타입 변환 실패(?status=FOO 등)는 내부 메시지 대신 ApiStatus 기본 메시지를 쓴다
     */
    private fun validationMessage(ex: Exception): String? =
        when (ex) {
            is MethodArgumentNotValidException -> {
                ex.bindingResult.fieldErrors
                    .firstOrNull()
                    ?.takeUnless { it.isBindingFailure }
                    ?.defaultMessage
            }

            is HandlerMethodValidationException -> {
                ex.allErrors.firstOrNull()?.defaultMessage
            }

            is ConstraintViolationException -> {
                ex.constraintViolations.firstOrNull()?.message
            }

            else -> {
                null
            }
        }

    private fun respond(
        apiStatus: ApiStatus,
        request: HttpServletRequest,
        message: String? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(apiStatus.httpStatus)
            .body(ErrorResponse.of(apiStatus, request.method, request.requestURI, message ?: apiStatus.message))
}
