# Build stage: compile and package the application
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Download dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests need a database and run in CI, not during the image build
RUN mvn -B -q package -DskipTests

# Runtime stage: only the JRE and the packaged jar
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --no-create-home atlas
USER atlas

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
