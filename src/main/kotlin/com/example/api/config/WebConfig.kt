package com.example.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Spring MVC 설정
 */
@Configuration(proxyBeanMethods = false)
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverterFactory(CaseInsensitiveEnumConverterFactory())
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
