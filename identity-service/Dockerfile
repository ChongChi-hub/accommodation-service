FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/storage && chown -R appuser:appgroup /app
COPY --chown=appuser:appgroup identity-service/build/libs/identity-service-1.0.0-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

