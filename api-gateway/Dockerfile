# Multi-stage Dockerfile for api-gateway
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
    && ./gradlew :api-gateway:dependencies --no-daemon || true
COPY common/src ./common/src
COPY api-gateway/src ./api-gateway/src
RUN ./gradlew :api-gateway:bootJar -x test --no-daemon \
    && JAR=$(ls api-gateway/build/libs/api-gateway-*.jar | grep -v -- '-plain\.jar$' | head -n 1) \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:23-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone \
    && addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder --chown=appuser:appgroup /workspace/app.jar /app/app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
