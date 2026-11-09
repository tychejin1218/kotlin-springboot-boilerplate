# Kotlin Spring Boot 보일러플레이트

백엔드 API 서비스 개발을 위한 Kotlin Spring Boot 보일러플레이트 프로젝트입니다.
[Java 보일러플레이트](https://github.com/tychejin1218/spring-react-boilerplate)(springboot-boilerplate)와 같은 기능과 API 규격을 Kotlin 컨벤션으로 다시 작성했습니다.
JWT 인증, Redis 캐시, Read/Write DataSource 분리 등 실무 수준의 설정이 사전 구성되어 있고, `react-boilerplate` 를 그대로 붙일 수 있습니다.

Java 에서 넘어왔다면 [Java 개발자를 위한 Kotlin 가이드](docs/kotlin-for-java-developers.md)를 먼저 읽어보세요.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 / 프레임워크 | Kotlin 2.3.21, Java 25, Spring Boot 4.1.2 (Spring Framework 7, Jackson 3) |
| 빌드 | Gradle 9.3.0 (Kotlin DSL) |
| 데이터베이스 | MySQL 8.0 (Read/Write 분리, `LazyConnectionDataSourceProxy`) |
| ORM / 쿼리 | Spring Data JPA + [Kotlin JDSL](https://github.com/line/kotlin-jdsl) 3.9.0 |
| 캐시 | Redis 7.0 (로컬: 단일 노드, dev/stg/prd: 클러스터) |
| 인증 | JWT (JJWT 0.12.6) + Spring Security |
| 메일 | Spring Mail (로컬: MailHog) |
| 설정 암호화 | Jasypt 4.0.4 (PBEWITHHMACSHA512ANDAES_256) |
| 파일 스토리지 | AWS S3 (로컬: MinIO, 운영: IAM Role 기반 DefaultCredentialsProvider) |
| API 문서 | SpringDoc OpenAPI 3.2.0 (Swagger UI, local/dev/stg 만 활성) |
| 모니터링 | Actuator + Micrometer Prometheus, Prometheus/Grafana (compose) |
| 테스트 | JUnit 5, MockMvc Kotlin DSL, MockK/springmockk, Testcontainers |
| 코드 품질 | ktlint 1.8.0 + detekt 2.0.0-alpha.6 + JaCoCo |

---

## 프로젝트 구조

```
src/main/kotlin/com/example/api/
├── ApiApplication.kt
├── common/                  # 공통 모듈
│   ├── advice/              # ExceptionAdvice (전역 예외 처리)
│   ├── component/           # RedisComponent (문자열 연산, compare-and-set)
│   ├── controller/          # HealthController (/health)
│   ├── exception/           # ApiException
│   ├── response/            # BaseResponse, ErrorResponse, PageResponse (+ Page.toPageResponse)
│   └── type/                # ApiStatus (응답 코드 = enum 이름), RedisKeys (키 규칙)
├── config/                  # 설정
│   ├── aws/                 # S3Config, LocalS3Initializer (MinIO 버킷 생성)       [예제: file-upload]
│   ├── jwt/                 # JwtTokenProvider, JwtProperties
│   ├── redis/               # RedisConfig (클러스터 Replica 읽기), RedisPubSubConfig
│   ├── security/            # SecurityConfig, JwtAuthenticationFilter, LoginMember
│   ├── web/                 # TraceIdFilter (요청 추적 ID), LoggingFilter, RateLimitInterceptor
│   ├── AsyncConfig.kt       # @Async 활성화, MDC(traceId) 전파
│   ├── CacheConfig.kt       # Redis 캐시 (APP:CACHE:{캐시 이름}:{키})
│   ├── JasyptConfig.kt
│   ├── JpaConfig.kt         # Auditing (UTC)
│   ├── MainDataSourceConfig.kt
│   ├── OpenApiConfig.kt
│   └── WebConfig.kt         # CORS, Rate Limit 등록, 쿼리 파라미터 enum 변환
├── domain/                  # 도메인 공통
│   ├── entity/              # BaseAudit, Member, Todo (JPA Entity)
│   └── repository/          # MemberRepository, TodoRepository (JpaRepository + KotlinJdslJpqlExecutor)
├── auth/                    # 인증 모듈: AuthService, TokenService, EmailService, TempAuthService(local)
├── user/                    # 회원 모듈
├── todo/                    # 할 일 모듈                                           [예제: todo]
├── file/                    # Presigned URL 발급                                   [예제: file-upload]
├── notification/            # SSE + Redis Pub/Sub 알림                             [예제: notification]
└── admin/                   # 관리자 API                                           [예제: admin]
```

기능 패키지는 `controller/`(+ Swagger 어노테이션을 모은 `XxxControllerDocs`), `service/`, `dto/` 로 나눕니다.

**아키텍처 흐름**

```
Controller → Service → Repository (Spring Data JPA + Kotlin JDSL) → Database
```

**Read/Write DataSource 분리**

- `@Transactional(readOnly = true)` → Reader DataSource
- `@Transactional` → Writer DataSource

---

## Java 보일러플레이트와 다른 점

| Java 보일러플레이트 | 이 프로젝트 |
|---|---|
| Lombok `@Builder`/`@Getter` | 주 생성자 + named argument + 기본값 |
| `XxxDto` outer class + static inner class | `dto/XxxDto.kt` 에 top-level `data class` |
| `XxxDto.from(entity)` | 확장 함수 `fun Todo.toResponse()` |
| `Optional` | nullable 타입 + `?:` (`!!` 금지) |
| QueryDSL / MyBatis | Kotlin JDSL |
| `@Value` | `@ConfigurationProperties` data class (불변) |
| 인증 principal = 이메일 문자열 | `LoginMember(id, email)`. 서비스는 회원 번호로 동작 |
| Checkstyle + PMD | ktlint + detekt |
| Mockito | MockK / springmockk |
| 테스트 DB = 로컬 Docker MySQL·Redis | Testcontainers (테스트가 컨테이너를 직접 띄움) |

경로, 파라미터, 응답 JSON, 응답 코드는 Java 와 같습니다. Kotlin 쪽에만 `GET /api/todos/{id}` 가 더 있습니다.

---

## 로컬 개발 환경 설정

### 1. 사전 요구사항

- Java 25
- Docker / Docker Compose

### 2. Docker 실행 (MySQL + Redis + MailHog + MinIO)

```bash
docker compose up -d
```

Java 보일러플레이트 compose 와 동시에 띄울 수 있도록 호스트 포트를 비켜 두었습니다.

| 컨테이너 | 포트 | 접속 정보 |
|---------|------|---------|
| MySQL 8.0 | 3307 | DB: `boilerplate_db`, User: `boilerplate`, PW: `boilerplate` |
| Redis 7.0 | 6380 | 인증 없음 |
| MailHog | SMTP 1026, 웹 8026 | 받은 메일: http://localhost:8026 |
| MinIO | API 9010, 콘솔 9011 | `minioadmin` / `minioadmin` (콘솔: http://localhost:9011) |

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

- 기본 프로파일: `local`
- 서버 포트: `9091`
- Context-path: `/api`
- 전체 base URL: `http://localhost:9091/api`
- 시드 회원 (비밀번호 모두 `Test1234!`): `user1@example.com`, `user2@example.com`, `admin@example.com`(관리자)

앱까지 컨테이너로 띄우려면 `docker compose --profile app up -d --build` 를 씁니다 (Dockerfile, 비root 실행).

### 4. 커밋 전 검사 연결 (클론 후 한 번)

```bash
git config core.hooksPath .githooks
```

커밋할 때 Kotlin·Gradle 파일이 바뀌었으면 ktlint, detekt 를 돌리고 실패하면 커밋을 막습니다(`.githooks/pre-commit`).

---

## 환경 변수 / 프로파일

애플리케이션은 4개의 프로파일을 지원합니다.

| 프로파일 | 설명 | 로깅 |
|---------|------|------|
| `local` | 로컬 개발 | 콘솔 출력 |
| `dev` | 개발 서버 | 파일 출력 |
| `stg` | 스테이징 서버 | 파일 출력 |
| `prd` | 운영 서버 | 파일 출력 |

각 프로파일별 설정 파일 위치:

```
src/main/resources/
├── application.yml                         # 공통 설정 (포트, context-path, 세션 정책, Rate Limit 등)
└── config/
    ├── datasource/
    │   └── datasource-{profile}.yml        # DataSource (dev/stg/prd 는 환경변수)
    └── environment/
        └── environment-{profile}.yml       # CORS, JWT, Redis, 메일, S3, SpringDoc, Jasypt
```

**주요 환경 변수** (dev/stg/prd)

| 환경 변수 | 설명 | 기본값 |
|---------|------|------|
| `DB_WRITER_JDBC_URL`, `DB_READER_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD` | DataSource 접속 정보 | - |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 오리진 (쉼표 구분) | - |
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상). local 은 yml 에 고정 | - |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | Access 토큰 유효 기간. 미활동 만료보다 짧아야 함 | `900000` (15분) |
| `JWT_SESSION_IDLE_TIMEOUT_MS` | 미활동 만료. Refresh 토큰의 Redis TTL 이며 재발급마다 연장 | `1800000` (30분) |
| `JWT_SESSION_ABSOLUTE_TIMEOUT_MS` | 최초 로그인 기준 절대 만료 | `43200000` (12시간) |
| `REDIS_CLUSTER_NODES` | Redis 클러스터 노드 (예: `node1:6379,node2:6379`) | - |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP 접속 정보 | 포트 `587` |
| `AWS_REGION`, `AWS_S3_BUCKET_NAME` | S3 (자격증명은 IAM Role) | `ap-northeast-2` |
| `JASYPT_ENCRYPTOR_PASSWORD` | `ENC(...)` 값 복호화 비밀번호. local 은 `local-default` | - |
| `RATE_LIMIT_TRUST_FORWARDED_HEADERS` | X-Forwarded-For 를 클라이언트 IP 로 사용 (신뢰할 수 있는 프록시 뒤에서만) | `false` |

---

## API 목록

**인증 방식**: JWT Bearer Token (`Authorization: Bearer {accessToken}`)

응답 본문은 `{"statusCode": "OK", "message": "성공", "data": ...}` 형태이며, 오류 시 `statusCode` 에 `ApiStatus` enum 이름(`TODO_NOT_FOUND` 등)이 들어갑니다.

### 인증 API (`/api/auth`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| POST | `/api/auth/register` | 회원가입 (인증 메일 발송) | X |
| GET | `/api/auth/verify-email?token=` | 이메일 인증 후 토큰 발급 | X |
| POST | `/api/auth/resend-verification` | 인증 메일 재발송 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 (Refresh 토큰 무효화, Access 토큰 차단) | X |
| POST | `/api/auth/token/refresh` | Access/Refresh 토큰 재발급 (Refresh Token Rotation) | X |
| POST | `/api/auth/password/reset-request` | 비밀번호 재설정 링크 요청 | X |
| POST | `/api/auth/password/reset` | 비밀번호 재설정 | X |
| POST | `/api/auth/token/temp` | 임시 토큰 발급 (local 전용) | X |

로그인·회원가입은 IP·경로마다 분당 10회로 제한하고, 넘으면 429 `SYSTEM_RATE_LIMIT_EXCEEDED` 로 응답합니다.

### 회원 API (`/api/users`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/users/me` | 내 정보 조회 | O |

### 할 일 API (`/api/todos`) [예제: todo]

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/todos?page=0&size=10&status=all&sort=id` | 내 할 일 목록 (status: `all`/`completed`/`incomplete`, sort: `id`(최신순)/`title`) | O |
| GET | `/api/todos/{id}` | 할 일 단건 조회 | O |
| POST | `/api/todos` | 할 일 추가 | O |
| PATCH | `/api/todos/{id}/complete` | 할 일 완료 처리 (완료 알림 발송) | O |
| PATCH | `/api/todos/{id}/image` | 첨부 이미지 URL 저장 | O |
| DELETE | `/api/todos/{id}` | 할 일 삭제 (논리 삭제) | O |

목록은 Redis 에 30분 캐시하고, 할 일을 바꾸면 목록 캐시를 비웁니다.

### 파일 API (`/api/files`) [예제: file-upload]

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/files/presigned-url?fileName=&contentType=` | S3 업로드용 Presigned URL 발급 (이미지만) | O |

### 알림 API (`/api/notifications`) [예제: notification]

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/notifications/subscribe` | 실시간 알림 구독 (SSE, `text/event-stream`) | O |

### 관리자 API (`/api/admin`) [예제: admin]

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/admin/members` | 전체 회원 목록 조회 | O (ROLE_ADMIN) |

---

## 예제 기능 제거 방법

todo, 파일 업로드, notification, admin 은 참고용 예제입니다 (Java 보일러플레이트, FE 와 같은 구성). 공통 파일에 걸린 등록 지점에는 `[예제: ...]` 주석이 붙어 있어,
`grep -rn "\[예제: todo\]" .` 처럼 찾아 아래 디렉토리와 함께 지우면 제거됩니다.
제거 후 `./gradlew clean build` 로 컴파일, ktlint, detekt, 테스트가 모두 통과하는지 확인하세요.

경로는 `src/main/kotlin/com/example/api/` (테스트는 `src/test/kotlin/com/example/api/`) 기준입니다.

| 예제 기능 | 삭제할 디렉토리/파일 | 지울 등록 지점 (`[예제: ...]` 주석) |
|----------|-------------------|-------------------------------|
| todo (할 일 관리) | `todo/`, `domain/entity/Todo.kt`, `domain/repository/TodoRepository.kt`, 테스트 `todo/`, `domain/repository/TodoRepositoryTest.kt`, `AuthFlowTest` 의 할 일 테스트 | `ApiStatus.TODO_NOT_FOUND`, `OpenApiConfig.todoApi`, `CacheConfig.TODO_LIST`, `docker/init.sql` 의 todo DROP/CREATE/INSERT |
| 파일 업로드 (todo 이미지) | `file/`, `config/aws/`, 테스트 `file/` | `ApiStatus.FILE_CONTENT_TYPE_INVALID`, `OpenApiConfig.fileApi`, `build.gradle.kts` 의 `awssdk:s3`, `environment-{profile}.yml` 의 `aws.s3`, `docker-compose.yml` 의 `minio` 서비스와 `minio_data` 볼륨, app 의 `AWS_S3_ENDPOINT` 와 `depends_on`, `IntegrationTest` 의 `aws.s3.create-bucket-on-startup`. todo 를 남기고 업로드만 뺄 때는 `Todo.imageUrl`/`updateImage`, `TodoImageUpdateRequest`, `TodoResponse.imageUrl`, `PATCH /todos/{id}/image`(컨트롤러·Docs·서비스), `todo.image_url` 컬럼도 정리 |
| notification (SSE 알림) | `notification/`, `config/redis/RedisPubSubConfig.kt` | `RedisKeys.NOTIFICATION_CHANNEL`, `TodoService` 의 `TodoCompleteEvent` 발행 줄과 `eventPublisher` 필드·import |
| admin (관리자 API) | `admin/` | `OpenApiConfig.adminApi`. `MemberRole`, `@EnableMethodSecurity`, `ExceptionAdvice` 의 403 처리는 공통 기능이므로 유지 |

todo 와 notification 은 서로 엮여 있습니다. todo 를 지우면 `notification/event/TodoCompleteEvent` 를 받는 곳도 함께 지우거나 다른 이벤트로 바꿔야 합니다.
예제 통합 테스트는 `ExampleFeatureFlowTest` 에 모여 있으니 지운 예제의 테스트도 함께 지웁니다.

---

## Swagger UI

로컬 환경에서 Swagger UI 를 통해 API 를 테스트할 수 있습니다.

| 항목 | URL |
|------|-----|
| Swagger UI | http://localhost:9091/api/swagger-ui.html |
| API Docs (JSON) | http://localhost:9091/api/api-docs |

> Swagger 화면 우상단 **Authorize** 버튼에서 JWT 토큰을 입력하면 인증이 필요한 API 를 테스트할 수 있습니다.
> 로컬 환경에서만 `/api/auth/token/temp` 엔드포인트로 이메일만으로 임시 토큰을 발급받을 수 있습니다.

모든 응답에는 `X-Trace-Id` 헤더가 붙고, 같은 값이 로그 줄의 `[traceId]` 에 찍힙니다. 사용자가 전달한 ID 로 서버 로그를 바로 찾을 수 있습니다.

---

## 코드 품질 검사

```bash
# 포맷 자동 정렬 (코드 수정 후 항상 실행)
./gradlew ktlintFormat

# 포맷 검사
./gradlew ktlintCheck

# 코드 스멜 검사
./gradlew detekt
```

검사 결과 리포트 위치:
- ktlint: `build/reports/ktlint/`
- detekt: `build/reports/detekt/detekt.html`

설정은 `.editorconfig`(ktlint), `config/detekt/detekt.yml`(detekt) 에 있습니다.

---

## 테스트 실행

테스트는 Testcontainers 로 MySQL, Redis 컨테이너를 직접 띄웁니다. Docker 가 실행 중이어야 하며, 없으면 DB 통합 테스트는 건너뜁니다.

```bash
# 테스트 실행
./gradlew test

# 테스트 + JaCoCo 커버리지 리포트 생성 (라인 커버리지 50% 미만이면 실패, -PcoverageMinimum 으로 조정)
./gradlew clean test -Pcoverage
```

커버리지 리포트 위치:
- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`

---

## 빌드

```bash
# 전체 빌드 (컴파일 + ktlint + detekt + 테스트)
./gradlew build

# 빌드 산출물
# build/libs/app.jar
```

---

## Jasypt 암호화

`environment-{profile}.yml` 에 민감한 값을 `ENC(암호문)` 형태로 저장합니다.

### 새 ENC() 값 생성

```bash
./gradlew jasyptEncrypt --args="<비밀번호> <평문>"

# 예시
./gradlew jasyptEncrypt --args="local-default mySecretValue"
# 출력: XXXXXXXXXXXXXXXX... (암호문만 출력되므로 ENC(...) 로 감싸서 yml 에 적용)
```

> `JasyptConfig` 와 동일한 알고리즘(PBEWITHHMACSHA512ANDAES_256, RandomSalt/RandomIv, base64)으로 암호화합니다.
> 랜덤 Salt/IV 를 사용하므로 같은 평문이라도 실행할 때마다 다른 암호문이 나오며, 모두 정상 복호화됩니다.

### yml 에 적용

```yaml
jwt:
  secret: ENC(XXXXXXXXXXXXXXXX...)
```

> 로컬 기본 비밀번호: `local-default`
> 운영 환경에서는 `JASYPT_ENCRYPTOR_PASSWORD` 환경변수로 주입합니다.

---

## 모니터링

```bash
docker compose --profile monitoring up -d
```

| 항목 | URL |
|------|-----|
| Prometheus | http://localhost:9190 |
| Grafana | http://localhost:3001 (`admin` / `admin`, Prometheus 데이터 소스 자동 등록) |
| 메트릭 (앱) | http://localhost:9091/api/actuator/prometheus |

Prometheus 는 호스트의 `:9091` 을 수집하므로 앱을 `bootRun` 으로 띄우든 컨테이너로 띄우든 그대로 동작합니다.
예시 비즈니스 지표로 `todo_create_count_total`(할 일 생성 횟수)이 있습니다.

---

## Docker 볼륨 초기화

테이블 구조 변경 등으로 DB 를 초기화해야 할 때:

```bash
# 컨테이너 + 볼륨 삭제
docker compose down -v

# 재시작 (init.sql 자동 실행 → 테이블 재생성)
docker compose up -d
```

> `docker/init.sql` 이 MySQL 최초 기동 시 자동으로 실행됩니다. Testcontainers 테스트도 같은 파일로 테이블을 만듭니다.
> 볼륨을 지우지 않고 스키마만 다시 만들려면 `docker exec -i kotlin-boilerplate-mysql mysql -uroot -proot < docker/init.sql` 을 실행합니다 (테이블을 DROP 후 다시 만들므로 데이터는 지워집니다).
