package com.example.cars;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.*;
import java.util.List;

/**
 * 启动时从 car_clean.csv 加载数据到 H2 内存数据库
 * 仅在数据源为 H2 时执行，Hive 数据仓库不需要本地加载
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS cars (" +
        "  brand VARCHAR(100), " +
        "  level VARCHAR(50), " +
        "  energy_code INT, " +
        "  energy_name VARCHAR(50), " +
        "  fuel_grade INT, " +
        "  launch_date VARCHAR(30), " +
        "  launch_year INT, " +
        "  launch_month INT, " +
        "  launch_season VARCHAR(5), " +
        "  price_wan DOUBLE, " +
        "  warranty_years INT, " +
        "  warranty_km_wan DOUBLE, " +
        "  displacement_L DOUBLE, " +
        "  horsepower DOUBLE, " +
        "  engine_type VARCHAR(20), " +
        "  is_electric INT, " +
        "  gear_count INT, " +
        "  trans_type VARCHAR(20), " +
        "  doors INT, " +
        "  seats INT, " +
        "  body_type VARCHAR(30), " +
        "  body_structure VARCHAR(100)" +
        ")";

    private static final String INSERT_SQL =
        "INSERT INTO cars VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String[] COLUMNS = {
        "brand", "level", "energy_code", "energy_name", "fuel_grade",
        "launch_date", "launch_year", "launch_month", "launch_season", "price_wan",
        "warranty_years", "warranty_km_wan", "displacement_L", "horsepower", "engine_type",
        "is_electric", "gear_count", "trans_type", "doors", "seats",
        "body_type", "body_structure"
    };

    @Override
    public void run(String... args) throws Exception {
        // 如果数据源是 Hive，跳过本地数据加载
        String source = DataSourceConfig.getActiveSource();
        if ("hive".equals(source)) {
            log.info("数据源为 Hive，跳过本地 CSV 加载");
            return;
        }

        log.info("开始从 CSV 加载数据到 H2...");

        try (Connection conn = DataSourceConfig.getConnection()) {
            // 建表
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(CREATE_TABLE_SQL);
            }

            // 检查是否已有数据
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cars")) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    log.info("H2 已有 {} 条数据，跳过加载", rs.getInt(1));
                    return;
                }
            }

            // 从 classpath 读取 CSV（打包在 JAR 内）
            Reader csvReader = new InputStreamReader(
                new ClassPathResource("car_clean.csv").getInputStream(), "UTF-8");
            List<String[]> rows;
            try (CSVReader reader = new CSVReader(csvReader)) {
                rows = reader.readAll();
            }

            if (rows.size() <= 1) {
                log.warn("CSV 文件为空或只有表头");
                return;
            }

            // 批量插入（跳过表头行）
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                int batchCount = 0;
                for (int i = 1; i < rows.size(); i++) {
                    String[] cols = rows.get(i);
                    for (int j = 0; j < COLUMNS.length; j++) {
                        String val = j < cols.length ? cols[j].trim() : "";
                        setParameter(ps, j + 1, COLUMNS[j], val);
                    }
                    ps.addBatch();
                    batchCount++;
                    if (batchCount % 5000 == 0) {
                        ps.executeBatch();
                        conn.commit();
                        log.info("已加载 {} 条...", batchCount);
                    }
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
                log.info("CSV 数据加载完成，共 {} 条", batchCount);
            }
        }
    }

    private void setParameter(PreparedStatement ps, int idx, String colName, String val) throws SQLException {
        if (val.isEmpty() || "null".equalsIgnoreCase(val)) {
            ps.setNull(idx, Types.NULL);
            return;
        }
        switch (colName) {
            case "brand": case "level": case "energy_name": case "engine_type":
            case "trans_type": case "body_type": case "body_structure": case "launch_date":
            case "launch_season":
                ps.setString(idx, val);
                break;
            case "energy_code": case "fuel_grade": case "launch_year": case "launch_month":
            case "warranty_years": case "is_electric": case "gear_count": case "doors": case "seats":
                try { ps.setInt(idx, Integer.parseInt(val)); }
                catch (NumberFormatException e) { ps.setNull(idx, Types.NULL); }
                break;
            case "price_wan": case "warranty_km_wan": case "displacement_L": case "horsepower":
                try { ps.setDouble(idx, Double.parseDouble(val)); }
                catch (NumberFormatException e) { ps.setNull(idx, Types.NULL); }
                break;
            default:
                ps.setString(idx, val);
        }
    }
}
