## 프로젝트 개요

Spring Boot API 보일러플레이트 (Kotlin). Java 버전 `spring-react-boilerplate/springboot-boilerplate`의 설계를 따르되 Kotlin 관용 표현으로 작성한다.

- Kotlin 2.3.21 (Spring Boot BOM의 `kotlin.version`과 맞춤) / Java 25 / Spring Boot 4.1.2 / Gradle 9.3.0
- MySQL 8.0, JPA + MyBatis, Read/Write DataSource 분리

## 빌드 & 실행

```bash
./gradlew build          # 컴파일 + ktlint + detekt + 테스트
./gradlew ktlintFormat   # 포맷 자동 정렬 (코드 수정 후 항상 실행)
./gradlew test           # Testcontainers 사용, Docker 필요
docker compose up -d mysql && ./gradlew bootRun   # :9091, context-path /api
```

## 아키텍처

```
Controller → Service → Repository(JPA) / Mapper(MyBatis) → Database
```

- 기능 패키지: `com.example.api.{기능}/{controller,service,dto,repository,mapper}`
- Entity는 `domain/entity/`, JPA Repository는 `domain/repository/`
- MyBatis XML은 `resources/mapper/{기능}/`
- 조회 서비스 메서드는 `@Transactional(readOnly = true)` → Reader DB, 변경은 `@Transactional` → Writer DB
- JPA·MyBatis·트랜잭션 매니저는 Spring Boot 자동 구성을 쓴다. `MainDataSourceConfig`는 라우팅 DataSource만 정의한다

## Kotlin 작성 규칙 (Java 보일러플레이트와 다른 점)

| Java 보일러플레이트 | 이 프로젝트 |
|---|---|
| Lombok `@Builder`/`@Getter`/`@RequiredArgsConstructor` | 사용하지 않음. 주 생성자 + named argument + 기본값 |
| `XxxDto` outer class + static inner class | `{기능}/dto/XxxDto.kt` 파일 하나에 top-level `data class` 여러 개 |
| `of()` 정적 팩토리 | 생성자 + named argument. 변환은 확장 함수 `fun Todo.toResponse()` |
| `ModelMapper` | 사용하지 않음 (확장 함수로 명시적 변환) |
| QueryDSL | 사용하지 않음. 동적 쿼리는 MyBatis 또는 Spring Data JPA |
| Checkstyle + PMD | ktlint + detekt |
| Mockito / `@MockitoBean` | MockK / springmockk `@MockkBean` |
| `ApiException(HttpStatus, ApiStatus)` (기본 400) | `ApiException(ApiStatus)` — HTTP 상태 기본값은 `ApiStatus.httpStatus` |

- 의존성 주입은 생성자 주입만 쓰고 프로퍼티는 `private val`
- 컬렉션 반환 타입은 `List`/`Map` (Mutable 금지)
- null 이 올 수 있는 조회 결과는 반드시 nullable 타입(`TodoResponse?`)으로 선언하고 서비스에서 `?: throw ApiException(ApiStatus.NOT_FOUND)`
- 로거: `private val log = LoggerFactory.getLogger(javaClass)`, 메시지는 `{}` placeholder
- 컨트롤러 응답: `BaseResponse.ok(data)` / `BaseResponse.ok()` (반환 타입 `BaseResponse<Unit>`)
- 요청 DTO 검증: `@field:` 없이 `@NotBlank(message = "...")` 사용 가능 (`-Xannotation-default-target=param-property`). 메시지는 오류 응답에 그대로 노출된다

### JPA Entity

- `data class` 금지 (equals/hashCode/toString이 연관관계·프록시와 충돌)
- `allOpen`·`plugin.jpa`가 build.gradle.kts에 설정되어 있으므로 `open`/기본 생성자를 직접 쓰지 않는다
- 상태 변경은 `var ... protected set` + 의미 있는 메서드(`complete()`, `softDelete()`)
- 감사 컬럼은 `BaseAudit` 상속 (UTC 기록)

### MyBatis

- 결과 DTO는 `val` data class 그대로 사용 (`arg-name-based-constructor-auto-mapping` + `javaParameters`)
- `resultType`/`parameterType`은 `com.example.api.**.dto` 패키지의 클래스 단순 이름(별칭)

## 테스트

- 함수명은 백틱 한글 문장 (`` fun `존재하지 않는 ID 면 404 를 반환한다`() ``), `@DisplayName` 불필요
- 컨트롤러: `@WebMvcTest` + `@MockkBean` + MockMvc Kotlin DSL (`mockMvc.get("/x").andExpect { ... }`)
- 서비스 단위: MockK
- DB 통합: `support/MySqlContainerTest` 상속 (Testcontainers MySQL, Docker 없으면 건너뜀)
- `@SpringBootTest`에서 실제 DB 데이터에 의존하는 단정 금지 — 테스트 안에서 데이터를 만든다
