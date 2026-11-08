FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY listing-service/build/libs/listing-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
