FROM openjdk:17-jdk-slim

# Устанавливаем curl для healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем jar файл
COPY build/libs/*.jar app.jar

# Создаем директорию для логов
VOLUME ["/app/logs"]

EXPOSE 8080

# Запускаем приложение с оптимизациями для PostgreSQL
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1