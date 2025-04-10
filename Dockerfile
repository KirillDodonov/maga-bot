# Этап сборки (если требуется собрать jar в контейнере)
FROM maven:3.8.6-eclipse-temurin-21 AS build
WORKDIR /app
# Копируем файлы проекта
COPY pom.xml .
# Скачиваем зависимости (опционально – можно использовать кэш)
RUN mvn dependency:go-offline
COPY . .
# Собираем jar-файл без тестов
RUN mvn clean package -DskipTests

# Финальный образ для запуска приложения
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Копируем готовый jar из предыдущего этапа (поменяйте имя файла, если оно отличается)
COPY --from=build /app/target/MagaTariffBot-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "maga-bot.jar"]