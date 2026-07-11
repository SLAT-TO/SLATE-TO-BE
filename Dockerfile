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

COPY --from=build /workspace/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
