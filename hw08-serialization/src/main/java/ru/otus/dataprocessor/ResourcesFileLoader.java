package ru.otus.dataprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.otus.model.Measurement;

@Getter
@Slf4j
public class ResourcesFileLoader implements Loader {

    private final String fileName;
    private final ObjectMapper mapper;

    public ResourcesFileLoader(String fileName) {
        this.fileName = fileName;
        this.mapper = JsonMapper.builder().build();
    }

    @Override
    public List<Measurement> load() {
        // читает файл, парсит и возвращает результат
        try {
            List<Measurement> measurements = mapper.readValue(
                    ResourcesFileLoader.class.getClassLoader().getResourceAsStream(this.fileName),
                    new TypeReference<>() {});
            log.info("Получили список: {}", measurements);
            return measurements;
        } catch (JsonProcessingException e) {
            log.error("Ошибка при парсинге JSON: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Общая ошибка: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
