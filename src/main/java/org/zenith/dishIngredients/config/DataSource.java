package org.zenith.dishIngredients.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DataSource {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DataSource() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        this.jdbcUrl = dotenv.get("JDBC_URL");
        this.username = dotenv.get("DB_USER");
        this.password = dotenv.get("PASSWORD");
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to close connection", e);
            }
        }
    }
}