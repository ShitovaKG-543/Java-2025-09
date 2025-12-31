-- Для @GeneratedValue(strategy = GenerationType.IDENTITY)
/*
create table client
(
    id   bigserial not null primary key,
    name varchar(50)
);

 */

-- Для @GeneratedValue(strategy = GenerationType.SEQUENCE)
-- Создание последовательности для таблицы client
CREATE SEQUENCE IF NOT EXISTS client_seq START WITH 1 INCREMENT BY 1;

-- Создание последовательности для таблицы address
CREATE SEQUENCE IF NOT EXISTS address_seq START WITH 1 INCREMENT BY 1;

-- Создание последовательности для таблицы phone
CREATE SEQUENCE IF NOT EXISTS phone_seq START WITH 1 INCREMENT BY 1;

-- Создание таблицы address (должна быть создана первой, так как на нее ссылается client)
CREATE TABLE IF NOT EXISTS address (
    id BIGINT NOT NULL,
    street VARCHAR(255),
    CONSTRAINT pk_address PRIMARY KEY (id)
);

-- Создание таблицы client (ссылается на address)
CREATE TABLE IF NOT EXISTS client (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    address_id BIGINT UNIQUE,
    CONSTRAINT pk_client PRIMARY KEY (id),
    CONSTRAINT fk_client_address FOREIGN KEY (address_id)
        REFERENCES address(id) ON DELETE CASCADE
);

-- Создание таблицы phone (ссылается на client)
CREATE TABLE IF NOT EXISTS phone (
    id BIGINT NOT NULL,
    number VARCHAR(255),
    client_id BIGINT,
    CONSTRAINT pk_phone PRIMARY KEY (id),
    CONSTRAINT fk_phone_client FOREIGN KEY (client_id)
        REFERENCES client(id) ON DELETE CASCADE
);

-- Создание индексов для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_client_address_id ON client(address_id);
CREATE INDEX IF NOT EXISTS idx_phone_client_id ON phone(client_id);

