package com.example.api.notification.dto

/** 할 일 완료 알림 (SSE `notification` 이벤트의 data) [예제: notification] */
data class TodoCompletedNotification(
    val todoId: Long,
    val todoTitle: String,
    val message: String = "\"$todoTitle\" 완료!",
)
