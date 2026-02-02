-- Создание таблицы address
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    street VARCHAR(50),
    client_id BIGINT NOT NULL
);

-- Создание таблицы client
CREATE TABLE client (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

-- Создание таблицы phone
CREATE TABLE phone (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(50),
    client_id BIGINT NOT NULL
);

-- Внешние ключи
ALTER TABLE address ADD CONSTRAINT fk_address_client
    FOREIGN KEY (client_id) REFERENCES client(id);

ALTER TABLE phone ADD CONSTRAINT fk_phone_client
    FOREIGN KEY (client_id) REFERENCES client(id);

-- Индексы
CREATE INDEX idx_address_client_id ON address(client_id);
CREATE INDEX idx_phone_client_id ON phone(client_id);