package com.example.api.notification.listener

import com.example.api.notification.dto.TodoCompletedNotification
import com.example.api.notification.event.TodoCompleteEvent
import com.example.api.notification.service.NotificationService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 도메인 이벤트 → 알림 [예제: notification]
 *
 * 커밋 전에 알림을 보내면 롤백됐을 때 잘못된 알림이 나가므로 커밋 뒤(AFTER_COMMIT)에 발행한다.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onTodoComplete(event: TodoCompleteEvent) {
        notificationService.publish(event.memberId, TodoCompletedNotification(event.todoId, event.todoTitle))
    }
}
