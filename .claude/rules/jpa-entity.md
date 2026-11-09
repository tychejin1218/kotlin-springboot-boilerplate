---
paths:
  - "src/main/kotlin/**/domain/entity/**/*.kt"
  - "docker/init.sql"
---
# JPA Entity

- `data class` 금지 (equals/hashCode/toString이 연관관계·프록시와 충돌)
- `allOpen`·`plugin.jpa`가 build.gradle.kts에 설정되어 있으므로 `open`/기본 생성자를 직접 쓰지 않는다
- 상태 변경은 `var ... protected set` + 의미 있는 메서드(`complete()`, `softDelete()`)
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)`. 필요 없는 양방향 매핑(`@OneToMany`)은 만들지 않는다
- 감사 컬럼은 `BaseAudit` 상속 (UTC 기록)
- 논리 삭제 대상은 `@SQLRestriction("deleted_at IS NULL")`을 붙여 모든 조회에서 제외한다
- 시간은 UTC. `ApiApplication.main`이 JVM 기본 시간대를 UTC 로 고정하므로 `hibernate.jdbc.time_zone`은 두지 않는다
- 테이블 DDL 은 `docker/init.sql`에 추가한다 (compose·Testcontainers 공용). 컬럼마다 `COMMENT`, 시각 컬럼은 `(UTC)` 를 적는다
