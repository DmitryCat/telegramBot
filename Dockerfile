
# --- Сборка приложения ---
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Запуск приложения ---
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Запуск
CMD ["java", "-jar", "app.jar"]