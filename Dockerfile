# Этап сборки (с JDK 21)
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Опционально: скачиваем зависимости, чтобы ускорить сборку
RUN mvn dependency:go-offline
COPY . .
# Собираем jar-файл без тестов
RUN mvn clean package -DskipTests

# Финальный образ (также с JDK 21)
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Обратите внимание: измените имя jar-файла, если требуется
COPY --from=build /app/target/MagaTariffBot-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "MagaTariffBot-0.0.1-SNAPSHOT.jar"]