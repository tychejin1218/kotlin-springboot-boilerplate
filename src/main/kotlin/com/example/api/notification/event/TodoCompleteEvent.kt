package com.example.api.notification.event

/**
 * 할 일 완료 이벤트 [예제: notification]
 *
 * TodoService 가 발행하고, 트랜잭션 커밋 뒤 NotificationEventListener 가 알림을 보낸다.
 */
data class TodoCompleteEvent(
    val memberId: Long,
    val todoId: Long,
    val todoTitle: String,
)
