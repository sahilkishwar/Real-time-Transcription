# Multi-Stage Build for Procucev AI Real-time Transcription Service
# Stage 1: Build the Application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as non-root user with full ownership of /app and persistent data directory
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    mkdir -p /app/data && \
    chown -R appuser:appgroup /app

# Copy jar from build stage with appuser ownership
COPY --from=build --chown=appuser:appgroup /app/target/realtime-transcription-service-*.jar /app/app.jar

USER appuser

# Cloud platforms pass PORT dynamic variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
