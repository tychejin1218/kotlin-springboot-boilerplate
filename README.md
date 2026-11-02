# kotlin-springboot-boilerplate

[springboot-boilerplate](https://github.com/tychejin1218/spring-react-boilerplate)(Java)를 Kotlin 관용 표현으로 옮긴 Spring Boot API 보일러플레이트

## 기술 스택

| 구분 | 내용 |
|---|---|
| 언어/런타임 | Kotlin 2.3.21 / Java 25 |
| 프레임워크 | Spring Boot 4.1.2 (Spring Framework 7, Jackson 3) |
| 빌드 | Gradle 9.3.0 (Kotlin DSL) |
| DB | MySQL 8.0, Read/Write DataSource 분리 (`LazyConnectionDataSourceProxy`) |
| 데이터 접근 | Spring Data JPA + MyBatis (같은 DataSource·트랜잭션 공유) |
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
│   ├── response/     BaseResponse, ErrorResponse, PageResponse
│   └── type/         ApiStatus (응답 코드 + 기본 HTTP 상태)
├── config/           MainDataSourceConfig (R/W 분리), JpaConfig (Auditing)
└── domain/entity/    BaseAudit (created_at/updated_at/deleted_at)
```

## 로드맵

1. ~~골격: Gradle Kotlin DSL, common, R/W DataSource, 정적 분석, Testcontainers~~
2. Todo 기능 (Entity → DTO → Repository/Mapper → Service → Controller → 테스트)
3. 인프라 기능: 인증/JWT → Redis/캐시 → S3/파일 → 알림
