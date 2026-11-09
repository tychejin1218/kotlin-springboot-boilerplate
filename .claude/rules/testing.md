---
paths:
  - "src/test/kotlin/**/*.kt"
---
# 테스트

- 함수명은 백틱 한글 문장 (`` fun `존재하지 않는 ID 면 404 를 반환한다`() ``), `@DisplayName` 불필요
- 컨트롤러: `@WebMvcTest` + `@ImportSecurityConfig`(실제 보안 설정 적용) + `@MockkBean` + MockMvc Kotlin DSL (`mockMvc.get("/x").andExpect { ... }`). 인증된 요청은 `@WithLoginMember(id = 회원 번호)` (`@WithMockUser` 는 principal 이 `LoginMember` 가 아니라서 쓰지 않는다)
- 서비스 단위: MockK. 엔티티 ID 는 `ReflectionTestUtils.setField(todo, "id", 1L)`로 넣는다
- Repository·DB·Redis 통합: `support/IntegrationTest` 상속 (Testcontainers MySQL + Redis, Docker 없으면 건너뜀). DB 만 쓰는 테스트는 클래스에 `@Transactional`(테스트마다 롤백). 메일은 베이스의 `emailService` 목으로 확인한다
- 여러 API 를 잇는 흐름(가입 → 인증 → 로그인 등)은 `@AutoConfigureMockMvc` + `IntegrationTest` 로 실제 MySQL·Redis 에 요청한다 (`auth/AuthFlowTest`, `ExampleFeatureFlowTest`)
- `IntegrationTest` 하위 클래스에 `@MockkBean`·`@DynamicPropertySource` 를 더하면 스프링 컨텍스트를 새로 띄워 느려진다. 목이 필요하면 베이스에 둔다
- `@SpringBootTest`에서 실제 DB 데이터에 의존하는 단정 금지 — 테스트 안에서 데이터를 만든다 (시드 회원으로 로그인만 하는 것은 허용)
- 비동기 결과(SSE, Pub/Sub)는 Awaitility(`await().atMost(...).untilAsserted { }`)로 기다린다
