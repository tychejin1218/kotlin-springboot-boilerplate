package com.example.api.common.controller

import com.example.api.common.response.BaseResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 헬스 체크 (로드밸런서용). 상세 상태는 /actuator/health
 */
@RestController
class HealthController {
    @GetMapping("/", "/health")
    fun health(): BaseResponse<Unit> = BaseResponse.ok()
}
