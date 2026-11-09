---
paths:
  - "src/main/kotlin/**/domain/repository/**/*.kt"
---
# Repository / Kotlin JDSL

- 단순 CRUD·메서드 이름 쿼리는 Spring Data JPA(`JpaRepository`). 단건은 nullable 반환(`fun findByEmail(email: String): Member?`)
- 동적 조건·DTO 프로젝션·조인 쿼리는 Kotlin JDSL. Repository 가 `KotlinJdslJpqlExecutor`를 함께 상속한다
- `whereAnd(...)`는 null 조건을 무시하므로 선택 검색 조건은 `?.let { ... }`으로 넘긴다
- 연관 엔티티 조건은 `path(Todo::member).path(Member::email)` 처럼 경로로 쓴다
- 정렬·페이지는 `Pageable`로 넘기고 쿼리 안에서 `orderBy`를 쓰지 않는다 (`findPage`가 Pageable 의 Sort 를 JPQL 에 붙인다)
- 목록은 `findPage(pageable) { ... }`(`Page<T>`) → `page.toPageResponse { it.toResponse() }`. `findAll { }`은 `List<T?>`를 돌려주므로 `filterNotNull()`
- 쿼리는 Repository 인터페이스의 default 메서드로 두고 서비스는 그 메서드만 호출한다

```kotlin
interface TodoRepository : JpaRepository<Todo, Long>, KotlinJdslJpqlExecutor {
    fun findTodoPage(email: String, completed: Boolean?, pageable: Pageable): Page<Todo> =
        findPage(pageable) {
            select(entity(Todo::class))
                .from(entity(Todo::class))
                .whereAnd(
                    path(Todo::member).path(Member::email).eq(email),
                    completed?.let { path(Todo::completed).eq(it) },
                )
        }
}
```
