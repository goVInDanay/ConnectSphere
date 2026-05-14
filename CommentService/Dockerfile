FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY target/*.jar app.jar

ENTRYPOINT [ "java", "-jar", "app.jar" ]