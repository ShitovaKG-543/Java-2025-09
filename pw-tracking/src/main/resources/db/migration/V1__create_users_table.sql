-- =====================================================
-- ТАБЛИЦА ПОЛЬЗОВАТЕЛЕЙ (users)
-- Хранит информацию о пользователях Telegram бота
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,                           -- Уникальный идентификатор пользователя (автоинкремент)
    chat_id BIGINT NOT NULL UNIQUE,                     -- ID чата в Telegram (уникальный, используется для отправки сообщений)
    username VARCHAR(100),                              -- Username пользователя в Telegram (может отсутствовать)
    first_name VARCHAR(100),                            -- Имя пользователя
    last_name VARCHAR(100),                             -- Фамилия пользователя
    language_code VARCHAR(10),                          -- Код языка пользователя (ru, en и т.д.)
    is_active BOOLEAN DEFAULT true,                     -- Статус активности (true - активен, false - заблокирован/удален)
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Дата и время регистрации в боте
    last_activity TIMESTAMP,                            -- Дата и время последней активности
    notification_enabled BOOLEAN DEFAULT true,          -- Флаг включения уведомлений (true - получает, false - не получает)
    timezone VARCHAR(50) DEFAULT 'Europe/Moscow',       -- Часовой пояс пользователя (для корректного отображения времени)
    last_notification_sent TIMESTAMP                    -- Время последней отправки массового уведомления
);

-- Индексы для таблицы пользователей
CREATE INDEX idx_users_chat_id ON users(chat_id);
CREATE INDEX idx_users_active ON users(is_active);