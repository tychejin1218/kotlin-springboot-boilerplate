package com.example.api.common.type

import org.springframework.http.HttpStatus

/**
 * API 응답 코드
 *
 * 8xx: 비즈니스 오류(ApiException), 9xx: 시스템 오류(ExceptionAdvice)
 *
 * @property httpStatus 이 코드로 응답할 때의 기본 HTTP 상태. ApiException 에서 다르게 지정할 수 있다
 */
enum class ApiStatus(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus,
) {
    OK("200", "성공", HttpStatus.OK),

    CUSTOM_EXCEPTION("800", "오류가 발생했습니다. 확인 후 다시 시도해주세요.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("801", "유효하지 않은 요청입니다.", HttpStatus.BAD_REQUEST),
    FORBIDDEN_REQUEST("802", "허용되지 않은 요청입니다.", HttpStatus.FORBIDDEN),
    DUPLICATED_REQUEST("803", "중복된 요청입니다.", HttpStatus.CONFLICT),
    NOT_FOUND("804", "존재하지 않는 정보입니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("805", "유효하지 않은 권한입니다.", HttpStatus.UNAUTHORIZED),

    INTERNAL_SERVER_ERROR("900", "내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    METHOD_ARGUMENT_NOT_VALID("901", "파라미터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_SERVLET_REQUEST_PARAMETER("902", "필수 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    CONSTRAINT_VIOLATION("903", "파라미터 유효성 검사에 실패했습니다.", HttpStatus.BAD_REQUEST),
    METHOD_ARGUMENT_TYPE_MISMATCH("904", "파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    NO_HANDLER_FOUND("905", "요청한 URL을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HTTP_REQUEST_METHOD_NOT_SUPPORTED("906", "지원하지 않는 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    HTTP_MEDIA_TYPE_NOT_SUPPORTED("907", "지원되지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    HTTP_MESSAGE_NOT_READABLE_EXCEPTION("908", "읽을 수 있는 요청 정보가 없습니다.", HttpStatus.BAD_REQUEST),
}
