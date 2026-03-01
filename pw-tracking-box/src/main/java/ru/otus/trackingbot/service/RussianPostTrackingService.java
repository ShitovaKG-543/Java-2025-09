package ru.otus.trackingbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.otus.trackingbot.config.BotConfig;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.util.TrackNumberValidator;

public class RussianPostTrackingService implements TrackingService {
    private final OkHttpClient client;
    private final Gson gson;
    private final BotConfig config;

    public RussianPostTrackingService(BotConfig config) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.config = config;
    }

    @Override
    public String getServiceName() {
        return "Почта России";
    }

    @Override
    public boolean canHandle(String trackNumber) {
        String carrier = TrackNumberValidator.detectCarrier(trackNumber);
        return carrier.equals("RUSSIAN_POST") || carrier.equals("INTERNATIONAL");
    }

    @Override
    public TrackingInfo track(String trackNumber) {
        TrackingInfo info = new TrackingInfo();
        info.setTrackNumber(trackNumber);
        info.setCarrier(getServiceName());

        try {
            // Проверяем наличие API ключа
            if (config.getRussianPostApiKey() == null
                    || config.getRussianPostApiKey().isEmpty()) {
                info.setSuccess(false);
                info.setErrorMessage("Не настроен API ключ Почты России");
                return info;
            }

            // Создаем JSON запрос для Почты России
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("operation", "get-all-events");

            JsonObject params = new JsonObject();
            params.addProperty("barcode", trackNumber);
            requestBody.add("params", params);

            Request request = new Request.Builder()
                    .url(config.getRussianPostTrackingUrl())
                    .header("Content-Type", "application/json;charset=UTF-8") // Исправлено!
                    .header("Authorization", "Bearer " + config.getRussianPostApiKey())
                    .post(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"), gson.toJson(requestBody)))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    info.setSuccess(false);

                    if (response.code() == 404) {
                        info.setErrorMessage("Посылка не найдена в системе Почты России");
                    } else if (response.code() == 401) {
                        info.setErrorMessage("Ошибка авторизации API Почты России");
                    } else {
                        info.setErrorMessage("Ошибка получения данных: " + response.code());
                    }
                    return info;
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (json.has("result")) {
                    JsonObject result = json.getAsJsonObject("result");

                    if (result.has("operations")) {
                        JsonArray operations = result.getAsJsonArray("operations");

                        if (operations.size() > 0) {
                            // Получаем последний статус
                            JsonObject lastOp =
                                    operations.get(operations.size() - 1).getAsJsonObject();

                            // Получаем локацию
                            String location = "Не указано";
                            if (lastOp.has("address")) {
                                JsonObject address = lastOp.getAsJsonObject("address");
                                if (address.has("description")) {
                                    location = address.get("description").getAsString();
                                } else if (address.has("address")) {
                                    location = address.get("address").getAsString();
                                }
                            }
                            info.setStatus(location);

                            // Получаем дату
                            if (lastOp.has("operationDateTime")) {
                                String dateStr = lastOp.get("operationDateTime").getAsString();
                                try {
                                    info.setLastUpdate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                                } catch (DateTimeParseException e) {
                                    // Пробуем другой формат
                                    try {
                                        dateStr = dateStr.replace(" ", "T");
                                        info.setLastUpdate(
                                                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                                    } catch (DateTimeParseException e2) {
                                        // Игнорируем
                                    }
                                }
                            }

                            // Получаем описание операции
                            if (lastOp.has("operationType")) {
                                JsonObject opType = lastOp.getAsJsonObject("operationType");
                                if (opType.has("name")) {
                                    info.setDescription(opType.get("name").getAsString());
                                }
                            }

                            // Добавляем всю историю
                            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                            for (int i = 0; i < operations.size(); i++) {
                                JsonObject op = operations.get(i).getAsJsonObject();

                                try {
                                    String dateStr = op.get("operationDateTime").getAsString();
                                    String location_op = "Не указано";

                                    if (op.has("address")) {
                                        JsonObject address = op.getAsJsonObject("address");
                                        if (address.has("description")) {
                                            location_op =
                                                    address.get("description").getAsString();
                                        } else if (address.has("address")) {
                                            location_op = address.get("address").getAsString();
                                        }
                                    }

                                    String status = "Неизвестно";
                                    if (op.has("operationType")) {
                                        JsonObject opType = op.getAsJsonObject("operationType");
                                        if (opType.has("name")) {
                                            status = opType.get("name").getAsString();
                                        }
                                    }

                                    String description = status;
                                    if (op.has("operationAttribute")) {
                                        JsonObject opAttr = op.getAsJsonObject("operationAttribute");
                                        if (opAttr.has("name")) {
                                            description = opAttr.get("name").getAsString();
                                        }
                                    }

                                    try {
                                        info.addHistory(
                                                LocalDateTime.parse(dateStr, formatter),
                                                status,
                                                location_op,
                                                description);
                                    } catch (DateTimeParseException e) {
                                        // Пропускаем записи с неверной датой
                                    }
                                } catch (Exception e) {
                                    // Пропускаем ошибочные записи
                                }
                            }
                        }
                    }

                    // Получаем информацию об отправителе и получателе
                    if (result.has("addressFrom")) {
                        JsonObject from = result.getAsJsonObject("addressFrom");
                        if (from.has("description")) {
                            info.setFromLocation(from.get("description").getAsString());
                        }
                    }

                    if (result.has("addressTo")) {
                        JsonObject to = result.getAsJsonObject("addressTo");
                        if (to.has("description")) {
                            info.setToLocation(to.get("description").getAsString());
                        }
                    }
                }

                if (info.getStatus() == null) {
                    info.setStatus("Информация отсутствует");
                }
            }

        } catch (Exception e) {
            info.setSuccess(false);
            info.setErrorMessage("Ошибка: " + e.getMessage());
        }

        return info;
    }
}
