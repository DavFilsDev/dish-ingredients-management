package org.zenith.dishIngredients.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    @Bean
    public DataSource dataSource() {
        String jdbcUrl = dotenv.get("JDBC_URL");
        String username = dotenv.get("DB_USER");
        String password = dotenv.get("PASSWORD");

        if (jdbcUrl == null || username == null || password == null) {
            throw new IllegalStateException("Missing database configuration in .env file");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}