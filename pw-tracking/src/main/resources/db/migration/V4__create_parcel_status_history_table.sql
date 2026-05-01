-- =====================================================
-- ТАБЛИЦА ИСТОРИИ СТАТУСОВ (parcel_status_history)
-- Все поля управляются приложением, БД не подставляет значения
-- =====================================================
CREATE TABLE IF NOT EXISTS parcel_status_history (
    id BIGSERIAL PRIMARY KEY,
    parcel_tracking_number VARCHAR(50) NOT NULL REFERENCES parcels(tracking_number) ON DELETE CASCADE,
    status_code VARCHAR(20),
    status_name VARCHAR(200) NOT NULL,
    status_description TEXT,
    operation_place VARCHAR(500),
    operation_date TIMESTAMP,
    weight INTEGER,
    created_at TIMESTAMP NOT NULL,
    is_current BOOLEAN NOT NULL
);

-- Индексы
CREATE INDEX idx_status_history_parcel ON parcel_status_history(parcel_tracking_number);
CREATE INDEX idx_status_history_parcel_date ON parcel_status_history(parcel_tracking_number, operation_date DESC);
CREATE INDEX idx_status_history_current ON parcel_status_history(parcel_tracking_number, is_current) WHERE is_current = true;