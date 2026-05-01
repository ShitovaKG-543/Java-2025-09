-- =====================================================
-- СВЯЗУЮЩАЯ ТАБЛИЦА (user_parcels)
-- Связывает пользователей и посылки (many-to-many с дополнительными атрибутами)
-- Каждая запись означает, что пользователь отслеживает конкретную посылку
-- =====================================================
CREATE TABLE IF NOT EXISTS user_parcels (
    id BIGSERIAL PRIMARY KEY,                           -- Уникальный идентификатор связи
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- ID пользователя
    parcel_tracking_number VARCHAR(50) NOT NULL REFERENCES parcels(tracking_number) ON DELETE CASCADE, -- Трек-номер посылки
    is_active BOOLEAN DEFAULT true,                     -- Флаг активности отслеживания (true - отслеживается, false - остановлено)
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Дата добавления посылки в отслеживание
    last_notification TIMESTAMP,                        -- Дата и время последнего отправленного уведомления
    notification_count INTEGER DEFAULT 0,               -- Общее количество отправленных уведомлений
    custom_name VARCHAR(200),                           -- Пользовательское имя для посылки
    last_status VARCHAR(200),                           -- Последний известный статус (дублируется для быстрого доступа)
    last_status_description TEXT,                       -- Подробное описание последнего статуса
    last_checked TIMESTAMP,                             -- Дата и время последней проверки статуса
    UNIQUE(user_id, parcel_tracking_number)             -- Уникальная пара (пользователь, посылка) - исключает дублирование
);

-- Индексы для связующей таблицы
CREATE INDEX idx_user_parcels_user_active ON user_parcels(user_id, is_active); -- Поиск активных посылок пользователя
CREATE INDEX idx_user_parcels_parcel ON user_parcels(parcel_tracking_number); -- Поиск всех пользователей, отслеживающих посылку
CREATE INDEX idx_user_parcels_last_checked ON user_parcels(last_checked) WHERE is_active = true; -- Для планировщика