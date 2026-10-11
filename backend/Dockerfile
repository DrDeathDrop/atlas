FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home atlas
USER atlas

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080 8081
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
