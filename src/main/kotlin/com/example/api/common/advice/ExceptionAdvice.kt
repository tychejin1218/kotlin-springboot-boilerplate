package com.example.api.common.advice

import com.example.api.common.exception.ApiException
import com.example.api.common.response.ErrorResponse
import com.example.api.common.type.ApiStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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
        return respond(ex.httpStatus, ex.apiStatus, request, ex.message)
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
                is MethodArgumentNotValidException -> ApiStatus.METHOD_ARGUMENT_NOT_VALID
                is HandlerMethodValidationException -> ApiStatus.CONSTRAINT_VIOLATION
                is ConstraintViolationException -> ApiStatus.CONSTRAINT_VIOLATION
                is MissingServletRequestParameterException -> ApiStatus.MISSING_SERVLET_REQUEST_PARAMETER
                is MethodArgumentTypeMismatchException -> ApiStatus.METHOD_ARGUMENT_TYPE_MISMATCH
                is NoHandlerFoundException -> ApiStatus.NO_HANDLER_FOUND
                is NoResourceFoundException -> ApiStatus.NO_HANDLER_FOUND
                is HttpRequestMethodNotSupportedException -> ApiStatus.HTTP_REQUEST_METHOD_NOT_SUPPORTED
                is HttpMediaTypeNotSupportedException -> ApiStatus.HTTP_MEDIA_TYPE_NOT_SUPPORTED
                else -> ApiStatus.HTTP_MESSAGE_NOT_READABLE_EXCEPTION
            }
        log.warn("{}: {} {} - {}", ex.javaClass.simpleName, request.method, request.requestURI, ex.message)
        return respond(apiStatus.httpStatus, apiStatus, request, validationMessage(ex))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Exception: {} {}", request.method, request.requestURI, ex)
        val apiStatus = ApiStatus.INTERNAL_SERVER_ERROR
        return respond(apiStatus.httpStatus, apiStatus, request)
    }

    /** DTO 에 적은 검증 메시지(@NotBlank(message = ...))를 응답에 그대로 노출한다 */
    private fun validationMessage(ex: Exception): String? =
        when (ex) {
            is MethodArgumentNotValidException -> {
                ex.bindingResult.fieldErrors
                    .firstOrNull()
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
        httpStatus: HttpStatus,
        apiStatus: ApiStatus,
        request: HttpServletRequest,
        message: String? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(httpStatus)
            .body(ErrorResponse.of(apiStatus, request.method, request.requestURI, message ?: apiStatus.message))
}
