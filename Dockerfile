# Stage 1: 빌드 (테스트·정적 분석은 CI 에서 ./gradlew build 로 돌리고 여기서는 jar 만 만든다)
FROM gradle:9.3.0-jdk25-alpine AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts .editorconfig ./
COPY config ./config
COPY src ./src
RUN gradle bootJar --no-daemon

# Stage 2: 실행 (root 가 아닌 사용자로 실행한다)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/app.jar app.jar
USER app
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
