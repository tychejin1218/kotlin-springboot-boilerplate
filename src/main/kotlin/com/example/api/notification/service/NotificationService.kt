package com.example.api.notification.service

import com.example.api.common.type.RedisKeys
import com.example.api.notification.dto.TodoCompletedNotification
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 실시간 알림 (SSE + Redis Pub/Sub) [예제: notification]
 *
 * 구독하면 그 회원의 Redis 채널(`APP:NOTIFICATION:CHANNEL:{회원 번호}`)을 듣다가 메시지가 오면 SSE 로 보낸다.
 * 발행은 Redis 채널로 하므로 SSE 를 연결한 서버가 어느 인스턴스든 알림이 전달된다.
 * 회원당 연결은 하나다. 새로 구독하면 이전 연결을 정리한다.
 */
@Service
class NotificationService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val listenerContainer: RedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    private val listeners = ConcurrentHashMap<Long, MessageListener>()

    fun subscribe(memberId: Long): SseEmitter {
        cleanup(memberId)

        val emitter = SseEmitter(SSE_TIMEOUT_MS)
        emitters[memberId] = emitter
        emitter.onCompletion { cleanup(memberId, emitter) }
        emitter.onTimeout { cleanup(memberId, emitter) }
        emitter.onError { cleanup(memberId, emitter) }

        val listener = MessageListener { message, _ -> send(memberId, String(message.body)) }
        listeners[memberId] = listener
        listenerContainer.addMessageListener(listener, ChannelTopic(RedisKeys.NOTIFICATION_CHANNEL.key(memberId)))

        // 연결 직후 이벤트를 보내 클라이언트가 연결됐음을 알 수 있게 한다
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"))
        } catch (e: IOException) {
            log.debug("SSE 연결 직후 전송 실패: memberId={} - {}", memberId, e.message)
            cleanup(memberId, emitter)
        }
        log.debug("SSE 구독 등록: memberId={}", memberId)
        return emitter
    }

    fun publish(
        memberId: Long,
        notification: TodoCompletedNotification,
    ) {
        try {
            stringRedisTemplate.convertAndSend(
                RedisKeys.NOTIFICATION_CHANNEL.key(memberId),
                objectMapper.writeValueAsString(notification),
            )
        } catch (e: DataAccessException) {
            // 알림은 부가 기능이라 실패해도 할 일 완료 처리를 되돌리지 않는다
            log.error("알림 발행 실패: memberId={}", memberId, e)
        }
    }

    private fun send(
        memberId: Long,
        data: String,
    ) {
        val emitter = emitters[memberId] ?: return
        try {
            emitter.send(SseEmitter.event().name("notification").data(data))
        } catch (e: IOException) {
            log.debug("SSE 전송 실패 (연결 종료): memberId={} - {}", memberId, e.message)
            cleanup(memberId, emitter)
        }
    }

    /** [emitter] 를 넘기면 그 연결이 아직 현재 연결일 때만 정리한다 (새 연결을 이전 연결의 콜백이 지우지 않도록) */
    private fun cleanup(
        memberId: Long,
        emitter: SseEmitter? = null,
    ) {
        val removed = if (emitter == null) emitters.remove(memberId) != null else emitters.remove(memberId, emitter)
        if (removed || emitter == null) {
            listeners.remove(memberId)?.let { listenerContainer.removeMessageListener(it) }
        }
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
