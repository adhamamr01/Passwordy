# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot fat jar ----
# Java 25 (LTS) to match pom.xml's java.version.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Maven wrapper + pom first so dependency resolution is cached as its own layer
# and only re-runs when the pom changes (not on every source edit).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Now the sources, then build (tests run in CI, not in the image build).
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080

# MaxRAMPercentage lets the JVM size its heap from the container's memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
