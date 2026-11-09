plugins {
    // Spring Boot 4.1.2 BOM이 관리하는 Kotlin 버전(kotlin.version)과 맞춘다
    val kotlinVersion = "2.3.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Spring의 null 안전성 어노테이션(JSpecify)을 Kotlin 타입에 엄격하게 반영
        // 생성자 파라미터의 어노테이션(@field:NotBlank 없이 @NotBlank)을 프로퍼티 필드에도 적용
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

// JPA 엔티티는 지연 로딩 프록시를 위해 open 이어야 한다 (plugin.spring 은 @Entity 를 열지 않음)
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // SpringDoc OpenAPI (Swagger UI). local/dev/stg 에서만 켠다 (application.yml 기본값은 꺼짐)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    // JWT
    val jjwtVersion = "0.12.6"
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Kotlin JDSL: 타입 안전한 JPQL 쿼리 (동적 쿼리 포함). Spring Boot 4 는 boot4-support 모듈을 쓴다
    val kotlinJdslVersion = "3.9.0"
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-boot4-support:$kotlinJdslVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("com.ninja-squad:springmockk:5.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
    archiveFileName = "app.jar"
}

tasks.jar {
    enabled = false
}

// 테스트는 항상 실행하고, JaCoCo 리포트/검증만 -Pcoverage 로 제어한다
// 예) ./gradlew clean test -Pcoverage -PcoverageMinimum=0.6
val coverageEnabled = project.hasProperty("coverage")
val coverageMinimum = (project.findProperty("coverageMinimum") as String?)?.toBigDecimal() ?: "0.50".toBigDecimal()
val coverageExcludes =
    listOf(
        "com/example/api/config/**",
        "com/example/api/domain/entity/**",
        "com/example/api/**/dto/**",
        "com/example/api/ApiApplication*",
    )

tasks.test {
    useJUnitPlatform()
    // main() 의 TimeZone.setDefault(UTC) 와 같은 조건으로 테스트한다
    systemProperty("user.timezone", "UTC")
    if (coverageEnabled) finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    onlyIf { coverageEnabled }
    dependsOn(tasks.test)
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(coverageExcludes) } })
    reports {
        html.required = true
        xml.required = true
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    onlyIf { coverageEnabled }
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(coverageExcludes) } })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = coverageMinimum
            }
        }
    }
}

// 정적 분석: ktlint(포맷) + detekt(코드 스멜). 설정은 .editorconfig, config/detekt/detekt.yml
ktlint {
    version = "1.8.0"
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
}

// detekt 는 자신이 빌드된 Kotlin 버전으로만 동작한다. Spring BOM 이 내리는 Kotlin 버전을 detekt 실행 classpath 에서만 되돌린다
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(
                dev.detekt.gradle.plugin
                    .getSupportedKotlinVersion(),
            )
        }
    }
}
