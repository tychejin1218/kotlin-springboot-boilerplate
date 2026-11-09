package com.example.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import java.time.ZoneOffset
import java.util.TimeZone

// JWT 로만 인증하므로 기본 사용자(콘솔에 찍히는 generated security password)를 만들지 않는다
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@ConfigurationPropertiesScan
class ApiApplication

fun main(args: Array<String>) {
    // 서버 시간대와 관계없이 UTC 로 동작시킨다. LocalDateTime 을 JDBC 로 보낼 때 JVM 시간대 기준 변환이 끼지 않게 한다
    // (테스트 JVM 은 build.gradle.kts 의 user.timezone 으로 같게 맞춘다)
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    runApplication<ApiApplication>(*args)
}
