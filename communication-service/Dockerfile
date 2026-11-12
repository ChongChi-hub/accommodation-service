FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/UTC /etc/localtime \
    && echo "UTC" > /etc/timezone
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --chown=appuser:appgroup communication-service/build/libs/communication-service-*.jar app.jar
USER appuser
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
