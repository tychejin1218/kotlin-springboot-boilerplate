package com.example.api.notification.controller

import com.example.api.notification.service.NotificationService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * 실시간 알림 구독 API [예제: notification]
 *
 * EventSource 는 Authorization 헤더를 못 보내므로 FE 는 fetch + ReadableStream 으로 구독한다.
 * 스트림 응답이라 Swagger 에서는 숨긴다.
 */
@Hidden
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(authentication: Authentication): SseEmitter = notificationService.subscribe(authentication.name)
}
