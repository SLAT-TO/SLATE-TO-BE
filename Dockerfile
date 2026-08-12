FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# LocalDateTime.now() (@CreatedDate 등)가 KST 벽시계를 쓰도록 JVM 기본 타임존을 고정한다.
# 미설정 시 alpine 기본값이 UTC라 저장·응답 시각이 9시간 뒤처진다. Temurin JRE는 tzdb 를 번들해 별도 tzdata 패키지가 필요 없다.
ENV TZ=Asia/Seoul

COPY --from=build /workspace/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
