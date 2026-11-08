FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY booking-service/build/libs/booking-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
