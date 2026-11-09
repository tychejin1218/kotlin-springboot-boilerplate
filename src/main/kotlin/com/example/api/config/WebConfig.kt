package com.example.api.config

import com.example.api.common.component.RedisComponent
import com.example.api.config.web.RateLimitInterceptor
import com.example.api.config.web.RateLimitProperties
import com.example.api.config.web.TraceIdFilter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper

/**
 * CORS 설정 (`cors.*`)
 *
 * @property allowedOrigins 허용할 오리진. allowCredentials=true 이므로 "*" 대신 오리진을 명시한다
 */
@ConfigurationProperties("cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
)

/**
 * Spring MVC 설정
 *
 * CORS 는 MVC 설정으로 등록한다. Spring Security 를 붙여도 `cors { }` 가 이 설정을 그대로 쓴다.
 * Rate Limit 은 무차별 대입·대량 가입을 막기 위해 로그인·회원가입에만 건다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties::class, RateLimitProperties::class)
class WebConfig(
    private val corsProperties: CorsProperties,
    private val rateLimitProperties: RateLimitProperties,
    private val redisComponent: RedisComponent,
    private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverterFactory(CaseInsensitiveEnumConverterFactory())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(RateLimitInterceptor(redisComponent, objectMapper, rateLimitProperties))
            .addPathPatterns("/auth/login", "/auth/register")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOriginPatterns(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods("*")
            .allowedHeaders("*")
            // 브라우저 JS 가 응답의 추적 ID 를 읽을 수 있도록 노출한다
            .exposedHeaders(TraceIdFilter.HEADER)
            .allowCredentials(true)
    }
}

/**
 * 쿼리 파라미터 → enum 변환 시 대소문자를 구분하지 않는다
 *
 * enum 상수는 Kotlin 컨벤션대로 대문자로 두고, API 는 Java 보일러플레이트·React 와 같은 소문자 값(`status=completed`)을 받는다.
 * 빈 값은 null 로 바꿔 DTO 의 기본값이 쓰이게 한다.
 */
class CaseInsensitiveEnumConverterFactory : ConverterFactory<String, Enum<*>> {
    // Converter<String, T?> 로 null(빈 값)을 돌려준다. 제네릭 소거로 런타임에는 같은 타입이다
    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<*>> getConverter(targetType: Class<T>): Converter<String, T> =
        Converter<String, T?> { source ->
            val value = source.trim()
            if (value.isEmpty()) {
                null
            } else {
                requireNotNull(targetType.enumConstants.firstOrNull { it.name.equals(value, ignoreCase = true) }) {
                    "${targetType.simpleName} 에 없는 값입니다: $value"
                }
            }
        } as Converter<String, T>
}
