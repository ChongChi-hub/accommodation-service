# ==========================================
# Multi-stage Dockerfile for Spring Boot Service
# ==========================================

# Stage 1: Build JAR using Gradle & Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Grant execution rights to gradlew
RUN chmod +x ./gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build bootJar
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image using minimal Temurin JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl

# Create non-root application user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for uploads
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

