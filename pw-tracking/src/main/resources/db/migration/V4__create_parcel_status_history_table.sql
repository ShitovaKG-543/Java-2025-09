-- =====================================================
-- ТАБЛИЦА ИСТОРИИ СТАТУСОВ (parcel_status_history)
-- Хранит полную историю изменения статусов для каждой посылки
-- =====================================================
CREATE TABLE IF NOT EXISTS parcel_status_history (
    id BIGSERIAL PRIMARY KEY,                           -- Уникальный идентификатор записи
    parcel_id BIGINT NOT NULL REFERENCES parcels(id) ON DELETE CASCADE, -- ID посылки (каскадное удаление при удалении посылки)
    status_code VARCHAR(20),                            -- Код статуса (внутренний код системы, например "5" для "Вручено")
    status_name VARCHAR(200) NOT NULL,                  -- Название статуса (человекочитаемое, например "Вручение адресату")
    status_description TEXT,                            -- Подробное описание статуса (может содержать дополнительную информацию)
    operation_place VARCHAR(500),                       -- Место совершения операции (город, индекс, отделение)
    operation_date TIMESTAMP,                           -- Дата и время совершения операции (из API)
    weight INTEGER,                                     -- Вес посылки на момент операции (в граммах, из API)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Дата создания записи в системе (когда мы узнали о статусе)
    is_current BOOLEAN DEFAULT false                    -- Флаг текущего статуса (true - актуальный статус, false - исторический)
);

-- Индексы для таблицы истории статусов
CREATE INDEX idx_status_history_parcel ON parcel_status_history(parcel_id);                    -- Быстрый поиск всей истории по посылке
CREATE INDEX idx_status_history_parcel_date ON parcel_status_history(parcel_id, operation_date DESC); -- Сортировка по дате операции (для отображения истории)
CREATE INDEX idx_status_history_current ON parcel_status_history(parcel_id, is_current);      -- Быстрый поиск текущего статуса посылки