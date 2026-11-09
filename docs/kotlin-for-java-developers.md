# Java 개발자를 위한 Kotlin 가이드

Java(Spring) 개발자가 이 프로젝트에서 Kotlin 으로 개발할 때 알아야 할 것을 정리한다.
프로젝트 규칙 요약은 [CLAUDE.md](../CLAUDE.md), 여기서는 "왜 그렇게 쓰는지"와 Java 와 다르게 동작해서 실수하기 쉬운 부분을 다룬다.

목차

1. [기본 문법: Java 와 1:1 대응](#1-기본-문법-java-와-11-대응)
2. [null 안전성](#2-null-안전성)
3. [클래스와 프로퍼티](#3-클래스와-프로퍼티)
4. [함수: 기본값·named argument·확장 함수·람다](#4-함수-기본값named-argument확장-함수람다)
5. [컬렉션](#5-컬렉션)
6. [식(expression) 중심 문법: if·when·try](#6-식expression-중심-문법-ifwhentry)
7. [스코프 함수](#7-스코프-함수)
8. [Spring 과 함께 쓸 때](#8-spring-과-함께-쓸-때)
9. [JPA Entity](#9-jpa-entity)
10. [테스트](#10-테스트)
11. [자주 하는 실수](#11-자주-하는-실수)
12. [도구와 학습 자료](#12-도구와-학습-자료)

---

## 1. 기본 문법: Java 와 1:1 대응

| Java | Kotlin | 비고 |
|---|---|---|
| `final String name = "a";` | `val name = "a"` | 타입 추론. 기본은 `val`(재할당 불가) |
| `String name = "a";` | `var name = "a"` | 꼭 필요할 때만 `var` |
| `public class A extends B implements C` | `class A : B(), C` | 상위 클래스는 생성자 호출 `()` 를 붙인다 |
| `public` (명시) | 생략 (기본이 `public`) | Java 의 package-private 은 없음. 대신 `internal`(모듈 범위) |
| `static` 메서드·상수 | top-level 함수, `companion object`, `const val` | |
| `void` | `Unit` (생략 가능) | |
| `Object` | `Any` | |
| `instanceof` + 캐스트 | `is` (스마트 캐스트) | `if (x is String) x.length` — 캐스트 불필요 |
| `(String) x` | `x as String` / `x as? String` | `as?` 는 실패 시 `null` |
| `a.equals(b)` | `a == b` | 참조 비교는 `===`. **`==` 가 equals 다** |
| `"a" + b + "c"` | `"a${b}c"`, `"a$b c"` | 문자열 템플릿 |
| `"""` 텍스트 블록 | `"""...""".trimIndent()` | |
| `switch` | `when` | 식으로 쓸 수 있고 `break` 없음 |
| `for (int i = 0; i < n; i++)` | `for (i in 0 until n)` / `repeat(n) { }` | |
| `throws IOException` | 없음 | Kotlin 에는 checked exception 이 없다 |
| `new Foo()` | `Foo()` | `new` 키워드 없음 |
| `Foo.class` | `Foo::class` (KClass), `Foo::class.java` (Class) | Spring API 는 대부분 `Class` 를 받는다 |
| `String[] args` | `Array<String>` | `vararg` 로 넘길 때는 spread `*args` |
| 세미콜론 | 생략 | |

```kotlin
// 함수: fun 이름(파라미터: 타입): 반환타입
fun add(a: Int, b: Int): Int = a + b   // 본문이 식 하나면 = 로 쓴다 (expression body)
```

`main` 도 클래스 밖 top-level 함수다 ([ApiApplication.kt](../src/main/kotlin/com/example/api/ApiApplication.kt)).

```kotlin
fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)   // SpringApplication.run(ApiApplication.class, args)
}
```

## 2. null 안전성

Kotlin 에서 가장 중요한 차이. **null 가능 여부가 타입에 들어간다.**

```kotlin
val a: String = "x"     // null 불가. a = null 은 컴파일 에러
val b: String? = null   // null 가능

b.length         // 컴파일 에러: null 일 수 있음
b?.length        // safe call: b 가 null 이면 결과도 null (Int?)
b?.length ?: 0   // 엘비스 연산자: null 이면 오른쪽 값
b!!.length       // null 이면 NPE. 이 프로젝트에서는 금지
```

Java `Optional` 은 쓰지 않는다. nullable 타입 + `?:` 가 그 역할을 한다.

```java
// Java
Todo todo = todoRepository.findById(id).orElseThrow(() -> new ApiException(...));
```

```kotlin
// Kotlin — Spring Data 가 제공하는 확장 함수 findByIdOrNull 사용
val todo = todoRepository.findByIdOrNull(id) ?: throw ApiException(ApiStatus.NOT_FOUND)
```

`throw` 와 `return` 도 식이라서 `?:` 오른쪽에 바로 쓸 수 있다.

### null 을 다루는 표준 함수

| 상황 | 쓰는 것 |
|---|---|
| 없으면 비즈니스 예외 | `?: throw ApiException(...)` |
| 프로그래밍 오류(있어야 하는데 없음) | `checkNotNull(x) { "메시지" }` → `IllegalStateException` |
| 인자 검증 | `requireNotNull(x)`, `require(조건)` → `IllegalArgumentException` |
| null 이 아닐 때만 실행 | `x?.let { ... }` |

예: 저장 전 엔티티는 `id` 가 `null` 이므로 응답 변환 시 `checkNotNull` 을 쓴다 ([TodoDto.kt](../src/main/kotlin/com/example/api/todo/dto/TodoDto.kt)).

### 플랫폼 타입 (Java 코드를 호출할 때)

Java 메서드의 반환값은 Kotlin 이 null 여부를 모르는 **플랫폼 타입(`String!`)** 이 된다. 컴파일러가 막아주지 않으므로 NPE 가 날 수 있다.

- Spring 7 은 JSpecify 어노테이션으로 null 여부를 표시하고, `-Xjsr305=strict`(build.gradle.kts) 로 이를 Kotlin 타입에 엄격하게 반영한다
- 어노테이션이 없는 Java 라이브러리 반환값은 받을 때 타입을 명시해 경계를 정한다: `val name: String? = javaObj.getName()`

## 3. 클래스와 프로퍼티

### 주 생성자와 프로퍼티

Java 의 필드 + 생성자 + getter/setter 를 한 줄로 쓴다.

```kotlin
class User(
    val name: String,        // val → 읽기 전용 프로퍼티 (getter 만)
    var age: Int,            // var → getter + setter
    nickname: String,        // val/var 없음 → 생성자 파라미터일 뿐, 프로퍼티 아님
)

user.name        // getName() 호출. Kotlin 에서는 필드 접근처럼 보이지만 실제로 getter
user.age = 20    // setAge(20)
```

Java 클래스의 getter/setter 도 Kotlin 에서는 프로퍼티처럼 쓴다: `page.content`, `page.totalElements`, `page.isLast`.

### Lombok 이 필요 없다

| Lombok | Kotlin |
|---|---|
| `@Getter` / `@Setter` | `val` / `var` |
| `@RequiredArgsConstructor` | 주 생성자 |
| `@Builder` | named argument + 기본값 (4장) |
| `@Data` / `@Value` | `data class` |
| `@Slf4j` | `private val log = LoggerFactory.getLogger(javaClass)` |

### data class

`equals`/`hashCode`/`toString`/`copy`/구조 분해를 자동 생성한다. DTO 에 쓴다.

```kotlin
data class TodoResponse(val id: Long, val title: String, val completed: Boolean)

val changed = response.copy(completed = true)   // 일부만 바꾼 새 객체
val (id, title) = response                      // 구조 분해
```

JPA Entity 에는 쓰지 않는다 (9장).

### 클래스는 기본이 final

Kotlin 클래스·메서드는 기본적으로 `final` 이다. 상속하려면 `open` 이 필요하다.
Spring AOP(`@Transactional` 프록시)와 JPA 지연 로딩은 상속이 필요해서 컴파일러 플러그인이 대신 열어준다 (8장).

### object / companion object

```kotlin
object JsonUtils { fun parse(...) }        // 싱글톤. JsonUtils.parse(...)

class PageResponse<T>(...) {
    companion object {                      // Java 의 static 멤버 자리
        fun <T> of(...): PageResponse<T> = ...
    }
}
PageResponse.of(...)
```

상수는 `const val MAX_SIZE = 100` (top-level 이나 `companion object` 안).
단순 유틸 함수는 굳이 `object` 로 감싸지 말고 top-level 함수로 둔다.

### enum class

생성자와 프로퍼티를 가질 수 있다 ([ApiStatus.kt](../src/main/kotlin/com/example/api/common/type/ApiStatus.kt)).

```kotlin
enum class TodoStatus(val completed: Boolean?) {
    ALL(null),
    COMPLETED(true),
    INCOMPLETE(false),
}
```

### sealed class / interface

하위 타입이 한정된 계층. `when` 에서 모든 경우를 다뤘는지 컴파일러가 검사한다 (Java 17 sealed 와 비슷).

```kotlin
sealed interface PaymentResult {
    data class Success(val id: String) : PaymentResult
    data class Failure(val reason: String) : PaymentResult
}

fun handle(r: PaymentResult) = when (r) {
    is PaymentResult.Success -> r.id         // 스마트 캐스트
    is PaymentResult.Failure -> r.reason
}                                            // else 불필요
```

## 4. 함수: 기본값·named argument·확장 함수·람다

### 기본값 + named argument = 빌더 대체

```kotlin
class ApiException(
    val apiStatus: ApiStatus,
    val httpStatus: HttpStatus = apiStatus.httpStatus,   // 다른 파라미터를 기본값으로 쓸 수 있다
    message: String = apiStatus.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

throw ApiException(ApiStatus.NOT_FOUND)
throw ApiException(ApiStatus.CUSTOM_EXCEPTION, message = "상세 메시지")
```

Java 의 오버로딩 생성자 여러 개, `@Builder` 가 모두 필요 없다.
파라미터가 많거나 같은 타입이 연달아 있으면 호출부에 이름을 붙인다: `Todo(title = request.title)`.

> Java 코드에서 Kotlin 의 기본값 함수를 호출해야 한다면 `@JvmOverloads` 를 붙인다. 이 프로젝트처럼 Kotlin 만 쓰면 필요 없다.

### 확장 함수

기존 클래스를 수정하지 않고 메서드를 추가한 것처럼 쓴다. `ModelMapper`·`XxxConverter` 대신 쓴다.

```kotlin
// TodoDto.kt
fun Todo.toResponse(): TodoResponse = TodoResponse(id = requireId(), title = title, completed = completed, imageUrl = imageUrl)

// PageResponse.kt — Spring 의 Page 에 변환 함수 추가
fun <T : Any, R : Any> Page<T>.toPageResponse(transform: (T) -> R): PageResponse<R> = ...

// 사용
todoRepository.findTodoPage(...).toPageResponse { it.toResponse() }
```

실제로는 첫 번째 인자로 수신 객체를 받는 static 함수로 컴파일된다. private 멤버에는 접근할 수 없고, 사용하는 쪽에서 import 해야 한다.

### 람다

```kotlin
list.map { it.title }                 // 파라미터가 하나면 it
list.map { todo -> todo.title }
list.filter { it.completed }.map(Todo::title)   // 함수 참조
```

마지막 파라미터가 함수면 괄호 밖으로 뺀다 (trailing lambda). Kotlin DSL(Kotlin JDSL, MockMvc DSL, Gradle Kotlin DSL)이 이 문법으로 만들어졌다.

```kotlin
findPage(pageable) {          // findPage(pageable, { ... }) 와 같다
    select(entity(Todo::class)).from(entity(Todo::class))
}
```

람다 안의 `return` 은 바깥 함수를 빠져나간다(inline 함수일 때). 람다만 빠져나가려면 `return@forEach` 처럼 라벨을 붙인다.

## 5. 컬렉션

- `List`/`Set`/`Map` 은 **읽기 전용 인터페이스**, 변경하려면 `MutableList` 등. 반환 타입은 읽기 전용으로 둔다
- 생성: `listOf(1, 2)`, `mutableListOf()`, `mapOf("a" to 1)`, `emptyList()`
- Stream API 대신 컬렉션 함수를 바로 쓴다 (`.stream()`·`collect` 불필요)

| Java Stream | Kotlin |
|---|---|
| `list.stream().map(f).collect(toList())` | `list.map(f)` |
| `filter(...).findFirst().orElse(null)` | `firstOrNull { ... }` |
| `anyMatch` / `allMatch` | `any { }` / `all { }` |
| `collect(groupingBy(...))` | `groupBy { }` |
| `collect(toMap(k, v))` | `associate { it.k to it.v }`, `associateBy { }` |
| `mapToInt(...).sum()` | `sumOf { }` |
| `flatMap(l -> l.stream())` | `flatMap { }` |

컬렉션 함수는 각 단계마다 새 리스트를 만든다(즉시 평가). 요소가 매우 많고 단계가 길면 `asSequence()` 로 지연 평가한다.

## 6. 식(expression) 중심 문법: if·when·try

`if`, `when`, `try` 가 값을 반환한다. 삼항 연산자(`? :`)는 없다.

```kotlin
val label = if (completed) "완료" else "미완료"

val httpStatus = when (code) {
    "804" -> HttpStatus.NOT_FOUND
    in listOf("801", "901") -> HttpStatus.BAD_REQUEST
    else -> HttpStatus.INTERNAL_SERVER_ERROR
}

val number = try { input.toInt() } catch (e: NumberFormatException) { 0 }
```

인자 없는 `when` 은 if-else 체인 대신 쓴다.

```kotlin
when {
    value.isEmpty() -> null
    value.length > 200 -> throw ApiException(ApiStatus.INVALID_REQUEST)
    else -> value
}
```

## 7. 스코프 함수

`let`, `apply`, `also`, `run`, `with`. 남용하면 읽기 어려워지므로 이 프로젝트는 아래 정도만 쓰고 **중첩하지 않는다.**

| 함수 | 용도 | 예 |
|---|---|---|
| `?.let { }` | null 이 아닐 때만 처리·변환 | `completed?.let { path(Todo::completed).eq(it) }` |
| `apply { }` | 객체 생성 후 설정 (자기 자신 반환) | `HikariDataSource().apply { maximumPoolSize = 10 }` |
| `also { }` | 부수 효과 후 그대로 반환 (로그·테스트 준비) | `todo.also { log.info("saved {}", it.id) }` |

## 8. Spring 과 함께 쓸 때

### 컴파일러 플러그인 (build.gradle.kts)

| 플러그인 | 하는 일 | 없으면 |
|---|---|---|
| `kotlin("plugin.spring")` | `@Component`·`@Transactional`·`@Configuration` 등이 붙은 클래스를 `open` 으로 만든다 | CGLIB 프록시 생성 실패, `@Transactional` 미동작 |
| `kotlin("plugin.jpa")` | `@Entity` 등에 인자 없는 생성자를 만들어준다 | Hibernate 가 엔티티를 생성 못 함 |
| `allOpen { annotation("jakarta.persistence.Entity") ... }` | Entity 를 `open` 으로 | 지연 로딩 프록시 생성 불가 |

그래서 코드에 `open` 이나 `protected constructor()` 를 직접 쓰지 않는다.

### 의존성 주입

생성자 주입만 쓴다. 생성자가 하나면 `@Autowired` 도 필요 없다.

```kotlin
@Service
class TodoService(
    private val todoRepository: TodoRepository,
)
```

`@Autowired lateinit var` 필드 주입은 쓰지 않는다 (테스트의 `@MockkBean` 제외).

### Spring 이 제공하는 Kotlin 확장

| Java | Kotlin |
|---|---|
| `SpringApplication.run(App.class, args)` | `runApplication<App>(*args)` |
| `repository.findById(id).orElse(null)` | `repository.findByIdOrNull(id)` |
| `mockMvc.perform(get("/x")).andExpect(status().isOk())` | `mockMvc.get("/x").andExpect { status { isOk() } }` |
| `restClient.get()...body(Foo.class)` | `.body<Foo>()` |

`reified` 제네릭 덕분에 `Foo::class.java` 를 넘기지 않아도 된다.

### 어노테이션 대상 (use-site target)

생성자의 `val` 하나가 Kotlin 에서는 **파라미터 + 필드 + getter** 로 컴파일된다. 어노테이션이 어디에 붙을지 모호하다.

- 이 프로젝트는 `-Xannotation-default-target=param-property` 를 켜서 `@NotBlank` 가 파라미터와 필드 모두에 붙는다. 그래서 `@field:NotBlank` 를 쓰지 않는다
- 필요하면 명시할 수 있다: `@field:`, `@get:`, `@param:`

```kotlin
data class TodoCreateRequest(
    @NotBlank(message = "할 일 내용을 입력해주세요.")
    val title: String,
)
```

### Jackson

`jackson-module-kotlin` 이 주 생성자·기본값·nullable 을 이해한다.

- JSON 에 필드가 없으면 기본값이 쓰인다
- 기본값 없는 non-null 프로퍼티가 JSON 에 없거나 `null` 이면 역직렬화 실패 → `HttpMessageNotReadableException`(908) 으로 응답된다. `@NotNull` 검증까지 가지 않는다는 점에 주의
- `is` 로 시작하는 Boolean 프로퍼티(`isDeleted`)는 JSON 이름이 바뀔 수 있으니 DTO 에서는 `completed` 처럼 `is` 없이 이름 짓는다

### 쿼리 파라미터 DTO

`@ModelAttribute` 로 바인딩되는 `data class` 는 기본값을 가진 생성자 파라미터로 기본값을 표현한다 ([TodoListRequest](../src/main/kotlin/com/example/api/todo/dto/TodoDto.kt)).

### 설정 프로퍼티

```kotlin
@ConfigurationProperties("app.jwt")
data class JwtProperties(
    val secret: String,
    val expirationSeconds: Long = 3600,
)
```

생성자 바인딩이 기본이고, `@ConfigurationPropertiesScan` 이 [ApiApplication](../src/main/kotlin/com/example/api/ApiApplication.kt) 에 있으므로 따로 등록하지 않는다.

### 로거

```kotlin
private val log = LoggerFactory.getLogger(javaClass)

log.info("todo 생성 id={}", todo.id)     // 문자열 템플릿 "$id" 대신 placeholder
```

## 9. JPA Entity

[Todo.kt](../src/main/kotlin/com/example/api/domain/entity/Todo.kt) 가 기준 예제다.

```kotlin
@Entity
@Table(name = "todo")
@SQLRestriction("deleted_at IS NULL")
class Todo(
    title: String,                     // 생성자 파라미터 (프로퍼티 아님)
) : BaseAudit() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null               // 저장 전에는 null
        protected set                  // 외부에서 변경 불가

    @Column(name = "title", nullable = false, length = 200)
    var title: String = title
        protected set

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false
        protected set

    fun complete() {                   // 상태 변경은 의미 있는 메서드로
        completed = true
    }
}
```

- **`data class` 금지**: 생성된 `equals`/`hashCode` 가 모든 필드를 쓰므로 지연 로딩 프록시·영속성 컨텍스트와 충돌하고, `toString` 이 연관관계를 따라가며 순환·추가 쿼리를 일으킨다
- `val` 대신 `var ... protected set`: Hibernate 가 값을 채워 넣어야 하고, 외부 변경은 막는다
- `id` 는 `Long?` 이고 `= null`. `0L` 로 두면 새 엔티티인지 판단(`isNew`)이 틀어질 수 있다
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)` + `lateinit var` 또는 nullable 로 둔다
- 컬렉션 연관관계는 `val items: MutableList<Item> = mutableListOf()` 로 두고 외부에는 `List` 로 노출한다

## 10. 테스트

| Java | Kotlin |
|---|---|
| `@DisplayName("...")` + `void test1()` | ``fun `존재하지 않는 ID 면 404 를 반환한다`()`` |
| Mockito `when(x.f()).thenReturn(y)` | MockK `every { x.f() } returns y` |
| `doNothing().when(x).f()` | `justRun { x.f() }` |
| `verify(x).f()` | `verify { x.f() }` |
| `ArgumentCaptor` | `slot<T>()` + `capture(slot)` |
| `@MockitoBean` | `@MockkBean` (springmockk) |
| `@Autowired` 필드 | 생성자 파라미터에 `@Autowired` |

```kotlin
@WebMvcTest(TodoController::class)
class TodoControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var todoService: TodoService

    @Test
    fun `없는 할 일을 조회하면 404 를 반환한다`() {
        every { todoService.getTodo(99) } throws ApiException(ApiStatus.NOT_FOUND)

        mockMvc.get("/todos/99").andExpect {
            status { isNotFound() }
            jsonPath("$.statusCode") { value(ApiStatus.NOT_FOUND.code) }
        }
    }
}
```

- Mockito 는 Kotlin 의 final 클래스·`any()` null 문제 때문에 쓰지 않는다. MockK 는 Kotlin 용으로 만들어졌다
- `mockk<Foo>()` 는 기본이 strict: stub 하지 않은 함수를 호출하면 예외. 반환값이 상관없으면 `mockk<Foo>(relaxed = true)`
- 서비스 단위 테스트는 Spring 없이 `TodoService(mockk())` 로 바로 만든다 ([TodoServiceTest.kt](../src/test/kotlin/com/example/api/todo/service/TodoServiceTest.kt))
- `jsonPath("$.data")` 의 `$` 는 문자열 템플릿이 아니라 그대로 쓰인다(`$` 뒤에 식별자가 아니면 템플릿이 아님). 헷갈리면 `"\$.data"`

## 11. 자주 하는 실수

| 실수 | 결과 | 바르게 |
|---|---|---|
| `!!` 로 null 을 무시 | 런타임 NPE | `?:`, `checkNotNull`, `requireNotNull` |
| Java 메서드 반환값을 non-null 로 가정 | 플랫폼 타입이라 NPE | 받는 쪽에 타입 명시 |
| `==` 를 참조 비교로 생각 | Kotlin `==` 는 `equals` | 참조 비교는 `===` |
| Entity 를 `data class` 로 | 프록시·순환 참조 문제 | 일반 `class` |
| `lateinit var` 남용 | 초기화 전 접근 시 `UninitializedPropertyAccessException` | 생성자 주입, nullable |
| `private fun` 에 `@Transactional` | 프록시가 가로채지 못해 무시됨 (Java 와 같음) | public 메서드에. 같은 클래스 내부 호출도 프록시를 안 탐 |
| `List` 를 받아서 캐스트 후 수정 | 원본 공유·`UnsupportedOperationException` | `toMutableList()` 로 복사 |
| `apply`/`let`/`run` 중첩 | `it`/`this` 가 무엇인지 알 수 없음 | 지역 변수로 풀어 쓴다 |
| `Int` 와 `Long` 비교·대입 | 자동 형변환 없음, 컴파일 에러 | `1L`, `toLong()` |
| 문자열 템플릿으로 로그 | 로그 레벨과 상관없이 문자열 생성 | `{}` placeholder |
| `object` 에 상태 저장 | 싱글톤이라 요청 간 공유 | Spring 빈 + 상태 없음 |

## 12. 도구와 학습 자료

### 개발 도구

- `./gradlew ktlintFormat` 를 코드 수정 후 항상 실행. `./gradlew build` 가 ktlint + detekt 를 함께 검사한다
- IntelliJ **Code → Convert Java File to Kotlin File**(⌥⇧⌘K) 은 시작점일 뿐이다. `!!`, `lateinit`, `?.let` 남발이 섞여 나오므로 반드시 다듬는다
- **Tools → Kotlin → Show Kotlin Bytecode → Decompile** 로 Kotlin 코드가 어떤 Java 코드가 되는지 확인할 수 있다 (프로퍼티, 확장 함수, 기본값 동작을 이해할 때 유용)
- `.http` 파일이나 MockMvc 테스트로 API 를 확인한다

### 학습 순서 추천

1. [Kotlin Koans](https://play.kotlinlang.org/koans) — 브라우저에서 바로 푸는 연습 문제
2. [Kotlin 공식 문서: Java 개발자를 위한 가이드](https://kotlinlang.org/docs/java-to-kotlin-idioms-strings.html) — Java 관용구를 Kotlin 으로 옮기는 법
3. [Kotlin 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html) — ktlint 가 따르는 규칙
4. [Spring Framework Kotlin 지원](https://docs.spring.io/spring-framework/reference/languages/kotlin.html) / [Spring Boot Kotlin 지원](https://docs.spring.io/spring-boot/reference/features/kotlin.html)
5. [Kotlin JDSL](https://github.com/line/kotlin-jdsl) — 이 프로젝트의 동적 쿼리
6. [MockK](https://mockk.io/)
