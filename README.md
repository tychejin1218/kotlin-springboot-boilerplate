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
| 테스트 | JUnit 5, MockMvc Kotlin DSL, MockK/springmockk, Testcontainers |
| 정적 분석 | ktlint 1.8.0 + detekt 2.0.0-alpha.6, JaCoCo |

## 실행

```bash
docker compose up -d mysql    # 로컬 MySQL (localhost:3307)
./gradlew bootRun             # http://localhost:9091/api/health
```

## 빌드 & 검증

```bash
./gradlew build                      # 컴파일 + ktlint + detekt + 테스트
./gradlew test                       # 테스트 (Docker 필요, 없으면 DB 통합 테스트는 건너뜀)
./gradlew ktlintFormat               # 코드 포맷 자동 정렬
./gradlew detekt                     # 코드 스멜 검사
./gradlew clean test -Pcoverage      # 커버리지 리포트 + 최소 50% 검증 (-PcoverageMinimum=0.6)
```

## 프로파일

`local` | `dev` | `stg` | `prd` — `src/main/resources/config/datasource/datasource-{profile}.yml`

dev/stg/prd 접속 정보는 환경변수(`DB_WRITER_JDBC_URL`, `DB_READER_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`)로 주입합니다.

## 구조

```
src/main/kotlin/com/example/api/
├── ApiApplication.kt
├── common/
│   ├── advice/       ExceptionAdvice (전역 예외 처리)
│   ├── controller/   HealthController
│   ├── exception/    ApiException
│   ├── response/     BaseResponse, ErrorResponse, PageResponse (+ Page.toPageResponse)
│   └── type/         ApiStatus (응답 코드 + 기본 HTTP 상태)
├── config/           MainDataSourceConfig (R/W 분리), JpaConfig (Auditing)
├── domain/
│   ├── entity/       BaseAudit (created_at/updated_at/deleted_at), Todo
│   └── repository/   TodoRepository (JpaRepository + KotlinJdslJpqlExecutor)
└── todo/             controller / service / dto
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/todos?page=0&size=10&status=ALL&sort=ID` | 목록 (status: `ALL`/`COMPLETED`/`INCOMPLETE`, sort: `ID`(최신순)/`TITLE`) |
| GET | `/api/todos/{id}` | 단건 |
| POST | `/api/todos` | 생성 `{"title": "..."}` |
| PATCH | `/api/todos/{id}/complete` | 완료 처리 |
| DELETE | `/api/todos/{id}` | 논리 삭제 (`deleted_at`) |

스키마는 `docker/init.sql` 하나로 관리합니다. docker compose 와 Testcontainers 가 같은 파일로 테이블을 만듭니다.
이미 만들어진 로컬 볼륨에는 자동으로 다시 적용되지 않으므로 DDL 을 추가했다면 직접 실행합니다.

```bash
docker exec -i kotlin-boilerplate-mysql mysql -uroot -proot < docker/init.sql
```

## 로드맵

1. ~~골격: Gradle Kotlin DSL, common, R/W DataSource, 정적 분석, Testcontainers~~
2. ~~Todo 기능 (Entity → DTO → Repository(JPA + Kotlin JDSL) → Service → Controller → 테스트)~~
3. 인프라 기능: 인증/JWT → Redis/캐시 → S3/파일 → 알림
