FROM openjdk:17-jdk-slim

# Создаем директорию для приложения
WORKDIR /app

# Копируем jar файл
COPY build/libs/tracking-bot-1.0.0.jar app.jar

# Создаем директории для данных и логов
VOLUME ["/app/data", "/app/logs"]

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Добавляем healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1