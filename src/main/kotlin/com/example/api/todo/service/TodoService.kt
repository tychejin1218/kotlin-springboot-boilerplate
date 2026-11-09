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
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 할 일 서비스
 *
 * 모든 메서드는 로그인한 회원(email)이 소유한 할 일만 다룬다. 다른 회원의 할 일은 없는 것(TODO_NOT_FOUND)으로 본다.
 * 클래스 기본은 읽기 전용(Reader DB), 변경 메서드만 @Transactional(Writer DB)
 *
 * 목록은 회원·조회 조건별로 Redis 에 캐시하고, 변경하면 목록 캐시를 모두 비운다.
 * 캐시 키에는 이메일 대신 이메일의 해시를 쓴다 (Redis 키에 개인정보를 남기지 않는다).
 */
@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Cacheable(
        cacheNames = [CacheConfig.TODO_LIST],
        key =
            "T(org.springframework.util.DigestUtils).md5DigestAsHex(#email.getBytes())" +
                " + ':' + #request.page + ':' + #request.size + ':' + #request.status + ':' + #request.sort",
    )
    fun getTodoList(
        email: String,
        request: TodoListRequest,
    ): PageResponse<TodoResponse> =
        todoRepository
            .findTodoPage(
                email = email,
                completed = request.status.completed,
                pageable = PageRequest.of(request.page, request.size, request.sort.sort),
            ).toPageResponse { it.toResponse() }

    fun getTodo(
        email: String,
        id: Long,
    ): TodoResponse = findTodo(email, id).toResponse()

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun insertTodo(
        email: String,
        request: TodoCreateRequest,
    ): TodoResponse {
        // 토큰은 유효하지만 그 사이 탈퇴한 회원
        val member = memberRepository.findByEmail(email) ?: throw ApiException(ApiStatus.AUTH_UNAUTHORIZED)
        return todoRepository.save(Todo(title = request.title, member = member)).toResponse()
    }

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun updateTodoComplete(
        email: String,
        id: Long,
    ): TodoResponse {
        val todo = findTodo(email, id)
        todo.complete()
        // [예제: notification] 완료 알림. 커밋 뒤에 발행된다 (NotificationEventListener)
        eventPublisher.publishEvent(TodoCompleteEvent(checkNotNull(todo.member.id), checkNotNull(todo.id), todo.title))
        return todo.toResponse()
    }

    /** 첨부 이미지 변경 [예제: file-upload] */
    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun updateTodoImage(
        email: String,
        id: Long,
        request: TodoImageUpdateRequest,
    ): TodoResponse {
        val todo = findTodo(email, id)
        todo.updateImage(request.imageUrl)
        return todo.toResponse()
    }

    @CacheEvict(cacheNames = [CacheConfig.TODO_LIST], allEntries = true)
    @Transactional
    fun deleteTodo(
        email: String,
        id: Long,
    ) {
        findTodo(email, id).softDelete()
    }

    private fun findTodo(
        email: String,
        id: Long,
    ): Todo = todoRepository.findByIdAndMemberEmail(id, email) ?: throw ApiException(ApiStatus.TODO_NOT_FOUND)
}
