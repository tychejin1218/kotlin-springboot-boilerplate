package com.example.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import java.time.ZoneOffset
import java.util.TimeZone

@SpringBootApplication
@ConfigurationPropertiesScan
class ApiApplication

fun main(args: Array<String>) {
    // 서버 시간대와 관계없이 UTC 로 동작시킨다. LocalDateTime 을 JDBC 로 보낼 때 JVM 시간대 기준 변환이 끼지 않게 한다
    // (테스트 JVM 은 build.gradle.kts 의 user.timezone 으로 같게 맞춘다)
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    runApplication<ApiApplication>(*args)
}
