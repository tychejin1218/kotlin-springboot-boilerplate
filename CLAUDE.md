## 프로젝트 개요

Spring Boot API 보일러플레이트 (Kotlin). 구조는 Java 버전 `spring-react-boilerplate/springboot-boilerplate`를 참고했지만, 코드는 Kotlin 공식 코딩 컨벤션과 Spring 공식 Kotlin 가이드(start.spring.io 기본 설정)를 따른다.

- Kotlin 2.3.21 (Spring Boot BOM의 `kotlin.version`과 맞춤) / Java 25 / Spring Boot 4.1.2 / Gradle 9.3.0
- MySQL 8.0, Spring Data JPA + Kotlin JDSL, Read/Write DataSource 분리

## 빌드 & 실행

```bash
./gradlew build          # 컴파일 + ktlint + detekt + 테스트
./gradlew ktlintFormat   # 포맷 자동 정렬 (코드 수정 후 항상 실행)
./gradlew test           # Testcontainers 사용, Docker 필요
docker compose up -d mysql && ./gradlew bootRun   # :9091, context-path /api
```

## 아키텍처

```
Controller → Service → Repository(Spring Data JPA + Kotlin JDSL) → Database
```

- 기능 패키지: `com.example.api.{기능}/{controller,service,dto}`
- Entity는 `domain/entity/`, Repository는 `domain/repository/`
- 조회 서비스 메서드는 `@Transactional(readOnly = true)` → Reader DB, 변경은 `@Transactional` → Writer DB
- JPA·Kotlin JDSL·트랜잭션 매니저는 Spring Boot 자동 구성을 쓴다. `MainDataSourceConfig`는 라우팅 DataSource만 정의한다

## Kotlin 작성 규칙

Java 보일러플레이트에서 넘어올 때 바뀌는 점:

| Java 보일러플레이트 | 이 프로젝트 |
|---|---|
| Lombok `@Builder`/`@Getter`/`@RequiredArgsConstructor` | 사용하지 않음. 주 생성자 + named argument + 기본값 |
| `XxxDto` outer class + static inner class | `{기능}/dto/XxxDto.kt` 파일 하나에 top-level `data class` 여러 개 |
| `ModelMapper` | 사용하지 않음. Entity → DTO 변환은 확장 함수 `fun Todo.toResponse()` |
| `Optional` | nullable 타입 + `?:` |
| MyBatis / QueryDSL | Kotlin JDSL (아래) |
| Checkstyle + PMD | ktlint + detekt |
| Mockito / `@MockitoBean` | MockK / springmockk `@MockkBean` |
| `ApiException(HttpStatus, ApiStatus)` (기본 400) | `ApiException(ApiStatus)` — HTTP 상태 기본값은 `ApiStatus.httpStatus` |

- 단순 생성은 생성자 + named argument. 값을 계산해서 만들어야 할 때만 `companion object`의 팩토리 함수(`PageResponse.of(...)`)
- 의존성 주입은 생성자 주입만 쓰고 프로퍼티는 `private val`
- 컬렉션 반환 타입은 `List`/`Map` (Mutable 금지)
- null 이 올 수 있는 조회 결과는 nullable 타입(`Todo?`)으로 받고 서비스에서 `?: throw ApiException(ApiStatus.NOT_FOUND)`. `!!` 금지
- 스코프 함수는 객체 설정(`apply`)·null 처리(`?.let`) 정도로만 쓰고 중첩하지 않는다
- 로거: `private val log = LoggerFactory.getLogger(javaClass)`, 메시지는 `{}` placeholder
- API 규격(경로·파라미터 이름과 값·응답 JSON·응답 코드)은 Java 보일러플레이트와 같게 유지한다 (`react-boilerplate` 공용)
- 쿼리 파라미터용 enum 은 대문자 상수로 두고, API 문서·클라이언트는 소문자 값(`status=completed`)을 쓴다. `WebConfig`가 대소문자 구분 없이 변환한다
- 컨트롤러 응답: `BaseResponse.ok(data)` / `BaseResponse.ok()` (반환 타입 `BaseResponse<Unit>`)
- 요청 DTO 검증: `@field:` 없이 `@NotBlank(message = "...")` 사용 (`-Xannotation-default-target=param-property`, start.spring.io 기본값). 메시지는 오류 응답에 그대로 노출된다

### JPA Entity

- `data class` 금지 (equals/hashCode/toString이 연관관계·프록시와 충돌)
- `allOpen`·`plugin.jpa`가 build.gradle.kts에 설정되어 있으므로 `open`/기본 생성자를 직접 쓰지 않는다
- 상태 변경은 `var ... protected set` + 의미 있는 메서드(`complete()`, `softDelete()`)
- 감사 컬럼은 `BaseAudit` 상속 (UTC 기록)
- 논리 삭제 대상은 `@SQLRestriction("deleted_at IS NULL")`을 붙여 모든 조회에서 제외한다
- 시간은 UTC. `ApiApplication.main`이 JVM 기본 시간대를 UTC 로 고정하므로 `hibernate.jdbc.time_zone`은 두지 않는다
- 테이블 DDL 은 `docker/init.sql`에 추가한다 (compose·Testcontainers 공용)

### Repository / Kotlin JDSL

- 단순 CRUD·메서드 이름 쿼리는 Spring Data JPA(`JpaRepository`)
- 동적 조건·DTO 프로젝션·조인 쿼리는 Kotlin JDSL. Repository 가 `KotlinJdslJpqlExecutor`를 함께 상속한다
- `whereAnd(...)`는 null 조건을 무시하므로 선택 검색 조건은 `?.let { ... }`으로 넘긴다
- 정렬·페이지는 `Pageable`로 넘기고 쿼리 안에서 `orderBy`를 쓰지 않는다 (`findPage`가 Pageable 의 Sort 를 JPQL 에 붙인다)
- 목록은 `findPage(pageable) { ... }`(`Page<T>`) → `page.toPageResponse { it.toResponse() }`. `findAll { }`은 `List<T?>`를 돌려주므로 `filterNotNull()`
- 쿼리는 Repository 인터페이스의 default 메서드로 두고 서비스는 그 메서드만 호출한다

```kotlin
interface TodoRepository : JpaRepository<Todo, Long>, KotlinJdslJpqlExecutor {
    fun findTodoPage(completed: Boolean?, pageable: Pageable): Page<Todo> =
        findPage(pageable) {
            select(entity(Todo::class))
                .from(entity(Todo::class))
                .whereAnd(
                    completed?.let { path(Todo::completed).eq(it) },
                )
        }
}
```

## 테스트

- 함수명은 백틱 한글 문장 (`` fun `존재하지 않는 ID 면 404 를 반환한다`() ``), `@DisplayName` 불필요
- 컨트롤러: `@WebMvcTest` + `@MockkBean` + MockMvc Kotlin DSL (`mockMvc.get("/x").andExpect { ... }`)
- 서비스 단위: MockK. 엔티티 ID 는 `ReflectionTestUtils.setField(todo, "id", 1L)`로 넣는다
- Repository·DB 통합: `support/MySqlContainerTest` 상속 + 클래스에 `@Transactional`(테스트마다 롤백) (Testcontainers MySQL, Docker 없으면 건너뜀)
- `@SpringBootTest`에서 실제 DB 데이터에 의존하는 단정 금지 — 테스트 안에서 데이터를 만든다
