package com.example.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc OpenAPI 설정
 *
 * 활성화 여부는 `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled` 로 정한다.
 * application.yml 기본값은 꺼짐이고 local/dev/stg 의 environment-{profile}.yml 에서만 켠다. prd 는 꺼진 채로 둔다.
 *
 * Swagger UI 우상단 Authorize 에 Access 토큰을 넣으면 모든 요청에 `Authorization: Bearer {token}` 이 붙는다.
 * 기능 패키지마다 [GroupedOpenApi] 빈을 추가해 Swagger UI 에서 그룹으로 나눈다.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("API")
                    .version("v1.0.0")
                    .description("백엔드 API 문서"),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_AUTH,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))

    @Bean
    fun authApi(): GroupedOpenApi = group("인증 API", "/auth/**")

    @Bean
    fun userApi(): GroupedOpenApi = group("회원 API", "/users/**")

    @Bean
    fun todoApi(): GroupedOpenApi = group("할 일 API", "/todos/**")

    private fun group(
        name: String,
        vararg paths: String,
    ): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(name)
            .pathsToMatch(*paths)
            .build()

    companion object {
        private const val BEARER_AUTH = "bearerAuth"
    }
}
