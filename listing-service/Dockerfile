# Multi-stage Dockerfile for listing-service
FROM eclipse-temurin:23-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common/build.gradle ./common/build.gradle
COPY api-gateway/build.gradle ./api-gateway/build.gradle
COPY identity-service/build.gradle ./identity-service/build.gradle
COPY listing-service/build.gradle ./listing-service/build.gradle
COPY booking-service/build.gradle ./booking-service/build.gradle
COPY billing-service/build.gradle ./billing-service/build.gradle
COPY communication-service/build.gradle ./communication-service/build.gradle
COPY admin-service/build.gradle ./admin-service/build.gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew :listing-service:dependencies --no-daemon || true
COPY common/src ./common/src
COPY listing-service/src ./listing-service/src
RUN ./gradlew :listing-service:bootJar -x test --no-daemon \
    && JAR=$(ls listing-service/build/libs/listing-service-*.jar | grep -v -- '-plain\.jar$' | head -n 1) \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:23-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/UTC /etc/localtime \
    && echo "UTC" > /etc/timezone \
    && addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/storage /app/uploads \
    && chown -R appuser:appgroup /app
COPY --from=builder --chown=appuser:appgroup /workspace/app.jar /app/app.jar
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
