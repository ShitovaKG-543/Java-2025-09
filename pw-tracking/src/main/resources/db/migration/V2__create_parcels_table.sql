-- =====================================================
-- ТАБЛИЦА ПОСЫЛОК (parcels)
-- Хранит базовую информацию о посылках (не зависит от пользователя)
-- Использует tracking_number в качестве первичного ключа (натуральный ключ)
-- =====================================================
CREATE TABLE IF NOT EXISTS parcels (
    tracking_number VARCHAR(50) PRIMARY KEY,            -- Трек-номер посылки (первичный ключ)
    service_name VARCHAR(50) NOT NULL,                  -- Название службы доставки (например, "Почта России")
    description TEXT,                                   -- Описание посылки (может содержать перечень вложений)
    weight DECIMAL(10, 2),                              -- Вес посылки в килограммах (с точностью до 2 знаков)
    estimated_delivery DATE,                            -- Ожидаемая дата доставки
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Дата создания записи в системе
    last_updated TIMESTAMP,                             -- Дата последнего обновления информации о посылке
    last_error TEXT,                                    -- Последняя ошибка при попытке получения информации
    retry_count INTEGER DEFAULT 0                       -- Количество попыток обновления после ошибки
);

-- Индексы для таблицы посылок
CREATE INDEX idx_parcels_service ON parcels(service_name);  -- Поиск по службе доставки (для статистики)