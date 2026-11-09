package com.example.api.todo.service

import com.example.api.common.exception.ApiException
import com.example.api.common.response.PageResponse
import com.example.api.common.response.toPageResponse
import com.example.api.common.type.ApiStatus
import com.example.api.config.CacheConfig
import com.example.api.domain.entity.Todo
import com.example.api.domain.repository.MemberRepository
import com.example.api.domain.repository.TodoRepository
import com.example.api.notification.event.TodoCompleteEvent
import com.example.api.todo.dto.TodoCreateRequest
import com.example.api.todo.dto.TodoImageUpdateRequest
import com.example.api.todo.dto.TodoListRequest
import com.example.api.todo.dto.TodoResponse
import com.example.api.todo.dto.toResponse
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 할 일 서비스
 *
 * 모든 메서드는 로그인한 회원(memberId)이 소유한 할 일만 다룬다. 다른 회원의 할 일은 없는 것(TODO_NOT_FOUND)으로 본다.
 * 클래스 기본은 읽기 전용(Reader DB), 변경 메서드만 @Transactional(Writer DB)
 *
 * 목록은 회원·조회 조건별로 Redis 에 캐시하고, 변경하면 목록 캐시를 모두 비운다.
 * 캐시 키는 회원 번호로 만든다 (Redis 키에 이메일 같은 개인정보를 남기지 않는다).
 */
@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
    meterRegistry: MeterRegistry,
) {
    // 비즈니스 지표 예시. /actuator/prometheus 에 todo_create_count_total 로 노출된다
    private val todoCreateCounter =
        Counter
            .builder("todo.create.count")
            .description("할 일 생성 횟수")
            .register(meterRegistry)

    @Cacheable(
        cacheNames = [CacheConfig.TODO_LIST],
        key = "#memberId + ':' + #request.page + ':' + #request.size + ':' + #request.status + ':' + #request.sort",
    )
    fun getTodoList(
        memberId: Long,
        request: TodoListRequest,
    ): PageResponse<TodoResponse> =
        todoRepository
            .findTodoPage(
                memberId = memberId,
                completed = request.status.completed,
                pageable = PageRequest.of(request.page, request.size, request.sort.sort),
            ).toPageResponse { it.toResponse() }

    fun getTodo(
        memberId: Long,
        id: Long,
    ): TodoResponse = findTodo(memberId, id).toResponse()

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun insertTodo(
        memberId: Long,
        request: TodoCreateRequest,
    ): TodoResponse {
        // 토큰은 유효하지만 그 사이 탈퇴한 회원
        val member = memberRepository.findByIdOrNull(memberId) ?: throw ApiException(ApiStatus.AUTH_UNAUTHORIZED)
        val todo = todoRepository.save(Todo(title = request.title, member = member))
        todoCreateCounter.increment()
        return todo.toResponse()
    }

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun updateTodoComplete(
        memberId: Long,
        id: Long,
    ): TodoResponse {
        val todo = findTodo(memberId, id)
        todo.complete()
        // [예제: notification] 완료 알림. 커밋 뒤에 발행된다 (NotificationEventListener)
        eventPublisher.publishEvent(TodoCompleteEvent(memberId, todo.requireId(), todo.title))
        return todo.toResponse()
    }

    /** 첨부 이미지 변경 [예제: file-upload] */
    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun updateTodoImage(
        memberId: Long,
        id: Long,
        request: TodoImageUpdateRequest,
    ): TodoResponse {
        val todo = findTodo(memberId, id)
        todo.updateImage(request.imageUrl)
        return todo.toResponse()
    }

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun deleteTodo(
        memberId: Long,
        id: Long,
    ) {
        findTodo(memberId, id).softDelete()
    }

    private fun findTodo(
        memberId: Long,
        id: Long,
    ): Todo = todoRepository.findByIdAndMemberId(id, memberId) ?: throw ApiException(ApiStatus.TODO_NOT_FOUND)
}
