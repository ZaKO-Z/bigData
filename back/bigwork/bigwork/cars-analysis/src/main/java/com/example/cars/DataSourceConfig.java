package com.example.cars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据源配置：Hive 优先，H2 降级
 * 1. 先尝试连接 Hive 数据仓库
 * 2. 如果 Hive 不可用，自动降级到 H2 内存数据库（启动时从 CSV 加载数据）
 *
 * 连接参数从 application.properties 读取：
 *   hive.url   - Hive JDBC 连接地址
 *   hive.user  - Hive 用户名
 *   hive.password - Hive 密码
 */
@Component
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private static final String H2_URL = "jdbc:h2:mem:carsdb;DB_CLOSE_DELAY=-1;MODE=MySQL";

    @Value("${hive.url:jdbc:hive2://121.4.19.133:10000/default}")
    private String hiveUrl;

    @Value("${hive.user:ubuntu}")
    private String hiveUser;

    @Value("${hive.password:}")
    private String hivePassword;

    private static String staticHiveUrl;
    private static String staticHiveUser;
    private static String staticHivePassword;

    private static volatile String activeSource = null;
    private static volatile boolean hiveChecked = false;

    @PostConstruct
    public void init() {
        staticHiveUrl = hiveUrl;
        staticHiveUser = hiveUser;
        staticHivePassword = hivePassword;
        log.info("Hive 配置加载完成: url={}, user={}", staticHiveUrl, staticHiveUser);
    }

    /**
     * 获取连接：Hive 优先，H2 降级
     */
    public static Connection getConnection() throws SQLException {
        // 如果还没检查过 Hive，先尝试
        if (!hiveChecked) {
            synchronized (DataSourceConfig.class) {
                if (!hiveChecked) {
                    if (tryHive()) {
                        activeSource = "hive";
                        log.info("数据源: Hive 数据仓库");
                    } else {
                        activeSource = "h2";
                        log.info("数据源: H2 本地数据库（Hive 不可用，已降级）");
                    }
                    hiveChecked = true;
                }
            }
        }

        if ("hive".equals(activeSource)) {
            try {
                return getHiveConnection();
            } catch (SQLException e) {
                log.warn("Hive 连接失败，降级到 H2: {}", e.getMessage());
                activeSource = "h2";
            }
        }
        return getH2Connection();
    }

    /**
     * 获取当前数据源名称
     */
    public static String getActiveSource() {
        return activeSource != null ? activeSource : "unknown";
    }

    /**
     * 尝试连接 Hive（3 秒超时）
     */
    private static boolean tryHive() {
        String url = staticHiveUrl != null ? staticHiveUrl : "jdbc:hive2://121.4.19.133:10000/default";
        String user = staticHiveUser != null ? staticHiveUser : "ubuntu";
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            // 设置登录超时
            DriverManager.setLoginTimeout(3);
            Connection conn = DriverManager.getConnection(url, user, "");
            // 验证连接可用
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                rs.next();
            }
            conn.close();
            log.info("Hive 连接测试成功");
            return true;
        } catch (Exception e) {
            log.warn("Hive 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    private static Connection getHiveConnection() throws SQLException {
        DriverManager.setLoginTimeout(5);
        String url = staticHiveUrl != null ? staticHiveUrl : "jdbc:hive2://121.4.19.133:10000/default";
        String user = staticHiveUser != null ? staticHiveUser : "ubuntu";
        return DriverManager.getConnection(url, user, "");
    }

    private static Connection getH2Connection() throws SQLException {
        return DriverManager.getConnection(H2_URL, "sa", "");
    }
}
