

# Etapa 1: build
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
WORKDIR /app/api
RUN gradle build -x test --no-daemon

# Etapa 2: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
