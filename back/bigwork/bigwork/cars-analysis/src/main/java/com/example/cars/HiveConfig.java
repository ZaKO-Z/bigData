package com.example.cars;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接配置
 * 优先 Hive 数据仓库，连接失败自动降级到 H2 本地数据库
 */
@Configuration
public class HiveConfig {

    @Bean
    public ConnectionProvider connectionProvider() {
        return new ConnectionProvider();
    }

    public static class ConnectionProvider {
        public Connection getConnection() throws SQLException {
            return DataSourceConfig.getConnection();
        }

        public String getActiveSource() {
            return DataSourceConfig.getActiveSource();
        }
    }
}
