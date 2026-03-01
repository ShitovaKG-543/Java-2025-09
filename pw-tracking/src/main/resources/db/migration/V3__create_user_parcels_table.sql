-- =====================================================
-- СВЯЗУЮЩАЯ ТАБЛИЦА (user_parcels)
-- Связывает пользователей и посылки (many-to-many с дополнительными атрибутами)
-- Каждая запись означает, что пользователь отслеживает конкретную посылку
-- =====================================================
CREATE TABLE IF NOT EXISTS user_parcels (
    id BIGSERIAL PRIMARY KEY,                           -- Уникальный идентификатор связи
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- ID пользователя (при удалении пользователя удаляются все его связи)
    parcel_id BIGINT NOT NULL REFERENCES parcels(id) ON DELETE CASCADE, -- ID посылки (при удалении посылки удаляются все связи)
    is_active BOOLEAN DEFAULT true,                     -- Флаг активности отслеживания (true - отслеживается, false - остановлено)
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Дата добавления посылки в отслеживание
    last_notification TIMESTAMP,                        -- Дата и время последнего отправленного уведомления по этой посылке
    notification_count INTEGER DEFAULT 0,               -- Общее количество отправленных уведомлений
    custom_name VARCHAR(200),                           -- Пользовательское имя для посылки (удобное название)
    last_status VARCHAR(200),                           -- Последний известный статус (дублируется для быстрого доступа)
    last_status_description TEXT,                       -- Подробное описание последнего статуса
    last_checked TIMESTAMP,                             -- Дата и время последней проверки статуса
    UNIQUE(user_id, parcel_id)                          -- Уникальная пара (пользователь, посылка) - исключает дублирование
);

-- Индексы для связующей таблицы
CREATE INDEX idx_user_parcels_user_active ON user_parcels(user_id, is_active); -- Поиск активных посылок пользователя
CREATE INDEX idx_user_parcels_parcel ON user_parcels(parcel_id);              -- Поиск всех пользователей, отслеживающих посылку