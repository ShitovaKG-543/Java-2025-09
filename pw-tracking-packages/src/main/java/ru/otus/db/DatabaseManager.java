package ru.otus.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    public static void init(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private static void createTables() {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS users (
                chat_id BIGINT PRIMARY KEY,
                username VARCHAR(255),
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String parcelsTable = """
            CREATE TABLE IF NOT EXISTS parcels (
                id SERIAL PRIMARY KEY,
                user_id BIGINT REFERENCES users(chat_id),
                tracking_number VARCHAR(50) NOT NULL,
                service_name VARCHAR(50) NOT NULL,
                last_status TEXT,
                last_status_update TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, tracking_number)
            )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(parcelsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}