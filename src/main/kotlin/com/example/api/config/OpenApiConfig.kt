package com.example.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc OpenAPI 설정
 *
 * 활성화 여부는 `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled` 로 정한다.
 * application.yml 기본값은 꺼짐이고 local/dev/stg 의 environment-{profile}.yml 에서만 켠다. prd 는 꺼진 채로 둔다.
 *
 * 기능 패키지마다 [GroupedOpenApi] 빈을 추가해 Swagger UI 에서 그룹으로 나눈다.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("API")
                .version("v1.0.0")
                .description("백엔드 API 문서"),
        )

    @Bean
    fun todoApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("할 일 API")
            .pathsToMatch("/todos/**")
            .build()
}
