## 프로젝트 개요

Spring Boot API 보일러플레이트 (Kotlin). 구조는 Java 버전 `spring-react-boilerplate/springboot-boilerplate`를 참고했지만, 코드는 Kotlin 공식 코딩 컨벤션과 Spring 공식 Kotlin 가이드(start.spring.io 기본 설정)를 따른다.

- Kotlin 2.3.21 (Spring Boot BOM의 `kotlin.version`과 맞춤) / Java 25 / Spring Boot 4.1.2 / Gradle 9.3.0
- MySQL 8.0, Spring Data JPA + Kotlin JDSL, Read/Write DataSource 분리

## 빌드 & 실행

```bash
./gradlew build          # 컴파일 + ktlint + detekt + 테스트
./gradlew ktlintFormat   # 포맷 자동 정렬 (코드 수정 후 항상 실행)
./gradlew test           # Testcontainers 사용, Docker 필요
docker compose up -d && ./gradlew bootRun   # MySQL·Redis·MailHog·MinIO, :9091, context-path /api
docker compose --profile monitoring up -d   # Prometheus(9190), Grafana(3001)
./gradlew jasyptEncrypt --args="<비밀번호> <평문>"   # yml 의 ENC(...) 값 생성
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

Java 개발자용 상세 설명은 `docs/kotlin-for-java-developers.md`. Java 보일러플레이트에서 넘어올 때 바뀌는 점:

| Java 보일러플레이트 | 이 프로젝트 |
|---|---|
| Lombok `@Builder`/`@Getter`/`@RequiredArgsConstructor` | 사용하지 않음. 주 생성자 + named argument + 기본값 |
| `XxxDto` outer class + static inner class | `{기능}/dto/XxxDto.kt` 파일 하나에 top-level `data class` 여러 개 |
| `ModelMapper` | 사용하지 않음. Entity → DTO 변환은 확장 함수 `fun Todo.toResponse()` |
| `Optional` | nullable 타입 + `?:` |
| MyBatis / QueryDSL | Kotlin JDSL (아래) |
| Checkstyle + PMD | ktlint + detekt |
| Mockito / `@MockitoBean` | MockK / springmockk `@MockkBean` |

- 단순 생성은 생성자 + named argument. 값을 계산해서 만들어야 할 때만 `companion object`의 팩토리 함수(`PageResponse.of(...)`)
- 의존성 주입은 생성자 주입만 쓰고 프로퍼티는 `private val`
- 컬렉션 반환 타입은 `List`/`Map` (Mutable 금지)
- null 이 올 수 있는 조회 결과는 nullable 타입(`Todo?`)으로 받고 서비스에서 `?: throw ApiException(ApiStatus.TODO_NOT_FOUND)`. `!!` 금지
- 스코프 함수는 객체 설정(`apply`)·null 처리(`?.let`) 정도로만 쓰고 중첩하지 않는다
- 로거: `private val log = LoggerFactory.getLogger(javaClass)`, 메시지는 `{}` placeholder
- API 규격(경로·파라미터 이름과 값·응답 JSON·응답 코드)은 Java 보일러플레이트와 같게 유지한다 (`react-boilerplate` 공용)
- 응답 코드(`statusCode`)는 `ApiStatus` enum 이름이며 FE 와의 규약이다. 이름은 Java 보일러플레이트 `ApiStatus` 와 같게 두고 접두 규칙(`TODO_*`, `VALIDATION_*`, `SYSTEM_*` 등)을 따른다
- 예외는 `throw ApiException(ApiStatus.XXX)`. HTTP 상태는 `ApiStatus.httpStatus` 에 묶여 있어 던지는 곳에서 지정하지 않는다
- 쿼리 파라미터용 enum 은 대문자 상수로 두고, API 문서·클라이언트는 소문자 값(`status=completed`)을 쓴다. `WebConfig`가 대소문자 구분 없이 변환한다
- 컨트롤러 응답: `BaseResponse.ok(data)` / `BaseResponse.ok()` (반환 타입 `BaseResponse<Unit>`)
- API 문서: Swagger 어노테이션은 `{기능}/controller/XxxControllerDocs` 인터페이스에 두고 컨트롤러가 구현한다(`override fun`). DTO 필드는 `@Schema(description, example)`, 쿼리 파라미터 DTO 는 `@ParameterObject`. 기능 패키지를 추가하면 `OpenApiConfig` 에 `GroupedOpenApi` 빈을 추가한다
- 프로파일별 설정: DataSource 는 `config/datasource/`, 그 밖의 환경 설정은 `config/environment/environment-{profile}.yml`
- 요청 DTO 검증: `@field:` 없이 `@NotBlank(message = "...")` 사용 (`-Xannotation-default-target=param-property`, start.spring.io 기본값). 메시지는 오류 응답에 그대로 노출된다
- 예제 기능(todo, 파일 업로드, notification, admin)이 공통 파일에 등록되는 곳에는 `[예제: 기능]` 주석을 붙이고, README 의 "예제 기능 제거 방법" 표를 함께 고친다

## 세부 규칙

`.claude/rules/` 의 규칙은 해당 경로의 파일을 다룰 때 자동으로 읽힌다.

- [인증 · Redis · 캐시](.claude/rules/security-cache.md): auth, controller, service, config, common
- [JPA Entity](.claude/rules/jpa-entity.md): domain/entity, docker/init.sql
- [Repository / Kotlin JDSL](.claude/rules/data-access.md): domain/repository
- [테스트](.claude/rules/testing.md): src/test

## 커밋

- `git config core.hooksPath .githooks` 를 한 번 해 두면 커밋할 때 `.githooks/pre-commit` 이 ktlint·detekt 를 돌린다 (테스트는 CI)
