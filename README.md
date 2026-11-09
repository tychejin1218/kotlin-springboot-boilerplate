# kotlin-springboot-boilerplate

[springboot-boilerplate](https://github.com/tychejin1218/spring-react-boilerplate)(Java)의 구조를 참고해 Kotlin 컨벤션으로 새로 작성한 Spring Boot API 보일러플레이트

## 기술 스택

| 구분 | 내용 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21 / Java 25 |
| 프레임워크 | Spring Boot 4.1.2 (Spring Framework 7, Jackson 3) |
| 빌드 | Gradle 9.3.0 (Kotlin DSL) |
| DB | MySQL 8.0, Read/Write DataSource 분리 (`LazyConnectionDataSourceProxy`) |
| 데이터 접근 | Spring Data JPA + [Kotlin JDSL](https://github.com/line/kotlin-jdsl) 3.9.0 (동적 쿼리) |
| 인증 | Spring Security + JWT (JJWT 0.12.6), Refresh 토큰은 Redis 7 |
| 메일 | Spring Mail (로컬: MailHog) |
| API 문서 | SpringDoc OpenAPI 3.2.0 (local/dev/stg) |
| 테스트 | JUnit 5, MockMvc Kotlin DSL, MockK/springmockk, Testcontainers |
| 정적 분석 | ktlint 1.8.0 + detekt 2.0.0-alpha.6, JaCoCo |

## 실행

```bash
docker compose up -d          # MySQL(3307), Redis(6380), MailHog(SMTP 1026, 웹 8026)
./gradlew bootRun             # http://localhost:9091/api/health
```

Java 보일러플레이트 compose 와 동시에 띄울 수 있도록 호스트 포트를 한 칸씩 비켜 두었습니다.

시드 회원 (비밀번호 모두 `Test1234!`): `user1@example.com`, `user2@example.com`, `admin@example.com`(관리자). 가입 인증 메일은 http://localhost:8026 에서 확인합니다.

## 빌드 & 검증

```bash
./gradlew build                      # 컴파일 + ktlint + detekt + 테스트
./gradlew test                       # 테스트 (Docker 필요, 없으면 DB 통합 테스트는 건너뜀)
./gradlew ktlintFormat               # 코드 포맷 자동 정렬
./gradlew detekt                     # 코드 스멜 검사
./gradlew clean test -Pcoverage      # 커버리지 리포트 + 최소 50% 검증 (-PcoverageMinimum=0.6)
```

## 프로파일

`local` | `dev` | `stg` | `prd` — 프로파일마다 두 파일을 읽습니다.

- `config/datasource/datasource-{profile}.yml`: DataSource
- `config/environment/environment-{profile}.yml`: CORS, SpringDoc 등 환경 설정

dev/stg/prd 접속 정보는 환경변수(`DB_WRITER_JDBC_URL`, `DB_READER_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`)로 주입합니다.
CORS 허용 오리진은 `CORS_ALLOWED_ORIGINS`(쉼표 구분)로 주입합니다.
그 밖에 `JWT_SECRET`, `REDIS_CLUSTER_NODES`, `MAIL_HOST` 등을 주입합니다(`environment-{profile}.yml` 참고).

| 환경 변수 | 설명 | 기본값 |
|---|---|---|
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상). local 은 yml 에 고정 | - |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | Access 토큰 유효 기간. 미활동 만료보다 짧아야 함 | 900000 (15분) |
| `JWT_SESSION_IDLE_TIMEOUT_MS` | 미활동 만료. Refresh 토큰 Redis TTL, 재발급마다 연장 | 1800000 (30분) |
| `JWT_SESSION_ABSOLUTE_TIMEOUT_MS` | 최초 로그인 기준 절대 만료 | 43200000 (12시간) |
| `RATE_LIMIT_TRUST_FORWARDED_HEADERS` | X-Forwarded-For 를 클라이언트 IP 로 사용 (신뢰할 수 있는 프록시 뒤에서만) | false |

## 구조

```
src/main/kotlin/com/example/api/
├── ApiApplication.kt
├── auth/             controller / service (AuthService, TokenService, EmailService) / dto
├── common/
│   ├── advice/       ExceptionAdvice (전역 예외 처리)
│   ├── component/    RedisComponent (문자열 연산, compare-and-set)
│   ├── controller/   HealthController
│   ├── exception/    ApiException
│   ├── response/     BaseResponse, ErrorResponse, PageResponse (+ Page.toPageResponse)
│   └── type/         ApiStatus (응답 코드 = enum 이름, HTTP 상태), RedisKeys (키 규칙)
├── config/           MainDataSourceConfig (R/W 분리), JpaConfig (Auditing), WebConfig (CORS, enum 변환),
│   │                 AsyncConfig (MDC 전파), OpenApiConfig (Swagger 그룹)
│   ├── jwt/          JwtTokenProvider, JwtProperties
│   ├── redis/        RedisConfig (클러스터 Replica 읽기)
│   ├── CacheConfig   Redis 캐시 (APP:CACHE:{캐시 이름}:{키}, 즉시 쓰기)
│   ├── security/     SecurityConfig, JwtAuthenticationFilter
│   └── web/          TraceIdFilter (X-Trace-Id, MDC), LoggingFilter (요청 요약 로그), RateLimitInterceptor
├── domain/
│   ├── entity/       BaseAudit (created_at/updated_at/deleted_at), Member, Todo
│   └── repository/   MemberRepository, TodoRepository (JpaRepository + KotlinJdslJpqlExecutor)
├── todo/             controller (+ ControllerDocs) / service / dto
└── user/             controller / service / dto
```

## Swagger UI / 추적 ID

- Swagger UI: http://localhost:9091/api/swagger-ui.html (local/dev/stg 만, prd 는 꺼짐). 로컬에서는 `POST /api/auth/token/temp` 에 이메일만 보내 토큰을 받고, 우상단 Authorize 에 넣으면 인증이 필요한 API 를 호출할 수 있습니다.
- 모든 응답에 `X-Trace-Id` 헤더가 붙고 같은 값이 로그 줄 `[traceId]` 에 찍힙니다. 요청에 형식에 맞는 `X-Trace-Id` 를 보내면 그 값을 이어서 씁니다.

## API

인증이 필요한 API 는 `Authorization: Bearer {accessToken}` 헤더를 보냅니다.

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/register` | 회원가입 (인증 메일 발송) | X |
| GET | `/api/auth/verify-email?token=` | 이메일 인증 후 토큰 발급 | X |
| POST | `/api/auth/resend-verification` | 인증 메일 재발송 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 (Refresh 토큰 무효화, Access 토큰 차단) | X |
| POST | `/api/auth/token/refresh` | 토큰 재발급 (Refresh Token Rotation) | X |
| POST | `/api/auth/password/reset-request` | 비밀번호 재설정 링크 요청 | X |
| POST | `/api/auth/password/reset` | 비밀번호 재설정 | X |
| POST | `/api/auth/token/temp` | 임시 토큰 발급 (local 전용) | X |
| GET | `/api/users/me` | 내 정보 | O |
| GET | `/api/todos?page=0&size=10&status=all&sort=id` | 내 할 일 목록 (status: `all`/`completed`/`incomplete`, sort: `id`(최신순)/`title`) | O |
| GET | `/api/todos/{id}` | 단건 | O |
| POST | `/api/todos` | 생성 `{"title": "..."}` | O |
| PATCH | `/api/todos/{id}/complete` | 완료 처리 | O |
| DELETE | `/api/todos/{id}` | 논리 삭제 (`deleted_at`) | O |

- 할 일 목록은 Redis 에 30분 캐시하고, 할 일을 바꾸면 목록 캐시를 비웁니다.
- 로그인·회원가입은 IP·경로마다 분당 10회로 제한하고, 넘으면 429 `SYSTEM_RATE_LIMIT_EXCEEDED` 로 응답합니다.

API 규격(경로·파라미터·응답 JSON·응답 코드)은 Java 보일러플레이트와 같게 유지해 `react-boilerplate`를 그대로 붙일 수 있게 한다.
응답 본문은 `{"statusCode": "OK", "message": "성공", "data": ...}` 형태이며, 오류 시 `statusCode` 에 `ApiStatus` enum 이름(`TODO_NOT_FOUND` 등)이 들어갑니다.

스키마는 `docker/init.sql` 하나로 관리합니다. docker compose 와 Testcontainers 가 같은 파일로 테이블을 만듭니다.
이미 만들어진 로컬 볼륨에는 자동으로 다시 적용되지 않으므로 DDL 을 바꿨다면 직접 실행합니다. 스크립트가 테이블을 DROP 후 다시 만들므로 로컬 데이터는 지워집니다.

```bash
docker exec -i kotlin-boilerplate-mysql mysql -uroot -proot < docker/init.sql
```

## 로드맵

1. ~~골격: Gradle Kotlin DSL, common, R/W DataSource, 정적 분석, Testcontainers~~
2. ~~Todo 기능 (Entity → DTO → Repository(JPA + Kotlin JDSL) → Service → Controller → 테스트)~~
3. ~~응답 코드를 Java 와 같은 `ApiStatus` enum 이름으로 통일~~
4. ~~웹 공통: 추적 ID, 요청 로그, CORS, SpringDoc, 비동기 MDC 전파~~
5. ~~인증: Member, Spring Security, JWT, Redis, 메일~~
6. ~~캐시, Rate Limit~~
7. 예제: 파일 업로드(S3), 알림(SSE), 관리자 API
8. 운영: Jasypt, Dockerfile, 모니터링
