---
paths:
  - "src/main/kotlin/**/auth/**/*.kt"
  - "src/main/kotlin/**/controller/**/*.kt"
  - "src/main/kotlin/**/service/**/*.kt"
  - "src/main/kotlin/**/config/**/*.kt"
  - "src/main/kotlin/**/common/**/*.kt"
---
# 인증 · Redis · 캐시

- 인증 principal 은 이메일이다. 컨트롤러는 `authentication: Authentication` 을 받아 `authentication.name` 을 서비스에 넘긴다(Docs 인터페이스에서는 `@Parameter(hidden = true)`)
- 인증 없이 열 경로는 `SecurityConfig.PERMIT_ALL_PATTERNS` 에 추가한다. 401/403 은 `ErrorResponse` JSON(`AUTH_UNAUTHORIZED`, `PERMISSION_FORBIDDEN`)
- 관리자 전용 API 는 메서드에 `@PreAuthorize("hasRole('ADMIN')")`. 거부되면 `ExceptionAdvice` 가 403 으로 바꾼다
- 소유자가 있는 데이터는 Repository 조건에 회원 이메일을 넣고, 남의 데이터는 `*_NOT_FOUND` 로 응답한다(존재 여부를 드러내지 않는다)
- 토큰 발급·재발급·폐기는 `TokenService` 로만 한다. Refresh 토큰은 회원 번호별로 Redis 에 하나만 두고 재발급마다 교체한다
- Redis 키는 `RedisKeys.XXX.key(식별자...)` 로만 만든다. 식별자에는 이메일 대신 회원 번호를 쓴다. 연산은 `RedisComponent`(장애 시 예외 대신 null/false)
- 로그에 이메일 같은 개인정보를 남기지 않는다. 회원은 MDC `memberId` 로 식별된다
- 캐시는 `@Cacheable(cacheNames = [CacheConfig.XXX])` + 변경 메서드에 `@CacheEvict`. 캐시 이름은 `CacheConfig` 상수로 둔다
  - 캐시 키에 이메일이 들어가면 해시로 바꾼다(`TodoService.getTodoList` 참고)
  - 캐시할 값의 타입은 `com.example.api` 패키지 클래스와 표준 컬렉션이어야 한다(`CacheConfig` 의 역직렬화 타입 제한)
- Rate Limit 경로는 `WebConfig.addInterceptors` 에 추가한다
- 커밋 뒤에 해야 하는 부수 효과(알림 등)는 이벤트를 발행하고 `@TransactionalEventListener(phase = AFTER_COMMIT)` 에서 처리한다
