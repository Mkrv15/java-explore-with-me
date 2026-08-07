FROM amazoncorretto:21.0.6
WORKDIR /app
COPY target/main-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]