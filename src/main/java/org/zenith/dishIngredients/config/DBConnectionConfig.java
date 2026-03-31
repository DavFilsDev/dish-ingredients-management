package org.zenith.dishIngredients.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zenith.dishIngredients.utils.DBConnection;

@Configuration
public class DBConnectionConfig {

    @Bean
    public DBConnection dbConnection() {
        return new DBConnection();
    }
}