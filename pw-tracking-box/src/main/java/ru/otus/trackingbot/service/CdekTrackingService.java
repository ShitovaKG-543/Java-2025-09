package ru.otus.trackingbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import okhttp3.*;
import ru.otus.trackingbot.config.BotConfig;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.util.TrackNumberValidator;

public class CdekTrackingService implements TrackingService {
    private final OkHttpClient client;
    private final Gson gson;
    private final BotConfig config;
    private String accessToken;
    private long tokenExpiryTime;

    public CdekTrackingService(BotConfig config) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.config = config;
    }

    @Override
    public String getServiceName() {
        return "СДЭК";
    }

    @Override
    public boolean canHandle(String trackNumber) {
        String carrier = TrackNumberValidator.detectCarrier(trackNumber);
        return carrier.equals("CDEK") || carrier.equals("UNKNOWN_FORMAT");
    }

    private synchronized void authenticate() throws IOException {
        // Проверяем, не истек ли токен (СДЭК токены живут 1 час)
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", config.getCdekClientId())
                .add("client_secret", config.getCdekClientSecret())
                .build();

        Request request = new Request.Builder()
                .url(config.getCdekAuthUrl())
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Ошибка аутентификации: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("access_token")) {
                accessToken = json.get("access_token").getAsString();
                // Токен действителен 3600 секунд (1 час)
                tokenExpiryTime = System.currentTimeMillis() + 3500 * 1000; // 58 минут
            } else {
                throw new IOException("Не удалось получить токен доступа");
            }
        }
    }

    @Override
    public TrackingInfo track(String trackNumber) {
        TrackingInfo info = new TrackingInfo();
        info.setTrackNumber(trackNumber);
        info.setCarrier(getServiceName());

        try {
            // Проверяем наличие client_id и client_secret
            if (config.getCdekClientId() == null
                    || config.getCdekClientId().isEmpty()
                    || config.getCdekClientSecret() == null
                    || config.getCdekClientSecret().isEmpty()) {
                info.setSuccess(false);
                info.setErrorMessage("Не настроены API ключи СДЭК");
                return info;
            }

            authenticate();

            String url = config.getCdekTrackingUrl() + "?tracking_code=" + trackNumber;

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    info.setSuccess(false);

                    if (response.code() == 404) {
                        info.setErrorMessage("Посылка не найдена в системе СДЭК");
                    } else if (response.code() == 401) {
                        info.setErrorMessage("Ошибка авторизации API СДЭК");
                    } else {
                        info.setErrorMessage("Ошибка получения данных: " + response.code());
                    }
                    return info;
                }

                String responseBody = response.body().string();
                JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();

                if (jsonArray.size() == 0) {
                    info.setSuccess(false);
                    info.setErrorMessage("Посылка не найдена");
                    return info;
                }

                JsonObject data = jsonArray.get(0).getAsJsonObject();

                // Парсим данные СДЭК
                if (data.has("statuses")) {
                    JsonArray statuses = data.getAsJsonArray("statuses");
                    if (statuses.size() > 0) {
                        JsonObject lastStatus =
                                statuses.get(statuses.size() - 1).getAsJsonObject();

                        if (lastStatus.has("name")) {
                            info.setStatus(lastStatus.get("name").getAsString());
                        }

                        if (lastStatus.has("date_time")) {
                            String dateStr = lastStatus.get("date_time").getAsString();
                            try {
                                info.setLastUpdate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                            } catch (DateTimeParseException e) {
                                // Игнорируем ошибку парсинга даты
                            }
                        }

                        if (lastStatus.has("description")
                                && !lastStatus.get("description").isJsonNull()) {
                            info.setDescription(lastStatus.get("description").getAsString());
                        }
                    }

                    // Добавляем историю
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    for (int i = 0; i < statuses.size(); i++) {
                        JsonObject status = statuses.get(i).getAsJsonObject();

                        try {
                            String dateStr = status.get("date_time").getAsString();
                            String statusName = status.get("name").getAsString();
                            String location = "Не указано";

                            if (status.has("city")) {
                                location = status.get("city").getAsString();
                            } else if (status.has("location")
                                    && !status.get("location").isJsonNull()) {
                                JsonObject loc = status.getAsJsonObject("location");
                                if (loc.has("city")) {
                                    location = loc.get("city").getAsString();
                                }
                            }

                            String description = status.has("description")
                                            && !status.get("description").isJsonNull()
                                    ? status.get("description").getAsString()
                                    : statusName;

                            info.addHistory(LocalDateTime.parse(dateStr, formatter), statusName, location, description);
                        } catch (Exception e) {
                            // Пропускаем ошибочные записи истории
                        }
                    }
                }

                if (data.has("from_location")) {
                    JsonObject fromLoc = data.getAsJsonObject("from_location");
                    if (fromLoc.has("city")) {
                        info.setFromLocation(fromLoc.get("city").getAsString());
                    }
                }

                if (data.has("to_location")) {
                    JsonObject toLoc = data.getAsJsonObject("to_location");
                    if (toLoc.has("city")) {
                        info.setToLocation(toLoc.get("city").getAsString());
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
