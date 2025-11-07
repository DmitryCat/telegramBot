
# --- Сборка ---
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app

# Копируем только pom и src
COPY pom.xml .
COPY src ./src

# Скачиваем зависимости заранее (ускоряет сборку)
RUN mvn dependency:go-offline

# Собираем jar, тесты пропускаем
RUN mvn clean package -DskipTests

# --- Запуск ---
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Копируем собранный jar из предыдущего слоя
COPY --from=build /app/target/*.jar app.jar

# Запуск бота
CMD ["java", "-jar", "app.jar"]