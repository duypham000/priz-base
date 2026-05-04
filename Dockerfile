FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/base-*.jar app.jar
EXPOSE 8083 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
