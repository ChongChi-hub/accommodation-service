FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY admin-service/build/libs/admin-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
