package com.example.cars;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class BusinessAnalysisService {

    private final HiveConfig.ConnectionProvider connectionProvider;

    public BusinessAnalysisService(HiveConfig.ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private List<Map<String, Object>> query(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
            return rows;
        }
    }

    // ==================== 1. 品牌 × 级别 × 指导价 ====================

    public List<Map<String, Object>> brandLevelPriceBand() throws SQLException {
        return query(
            "SELECT brand, level," +
            " ROUND(MIN(price_wan), 2) AS price_min," +
            " ROUND(MAX(price_wan), 2) AS price_max," +
            " ROUND(AVG(price_wan), 2) AS price_avg," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE price_wan IS NOT NULL AND level IS NOT NULL" +
            " GROUP BY brand, level" +
            " HAVING cnt >= 2" +
            " ORDER BY level, price_avg DESC"
        );
    }

    public List<Map<String, Object>> brandPriceByLevel(String level) throws SQLException {
        String safeLevel = level.replace("'", "''");
        return query(
            "SELECT brand," +
            " ROUND(MIN(price_wan), 2) AS price_min," +
            " ROUND(MAX(price_wan), 2) AS price_max," +
            " ROUND(AVG(price_wan), 2) AS price_avg," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE price_wan IS NOT NULL AND level = '" + safeLevel + "'" +
            " GROUP BY brand" +
            " ORDER BY price_avg DESC"
        );
    }

    public List<Map<String, Object>> levelBrandSummary() throws SQLException {
        return query(
            "SELECT level," +
            " COUNT(DISTINCT brand) AS brand_count," +
            " ROUND(MIN(price_wan), 2) AS price_min," +
            " ROUND(MAX(price_wan), 2) AS price_max," +
            " ROUND(AVG(price_wan), 2) AS price_avg," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE price_wan IS NOT NULL AND level IS NOT NULL" +
            " GROUP BY level" +
            " ORDER BY price_avg"
        );
    }

    // ==================== 2. 上市时间 × 能源类型 ====================

    public List<Map<String, Object>> energyTrendByYear() throws SQLException {
        return query(
            "SELECT launch_year, energy_name, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE launch_year IS NOT NULL AND energy_name IS NOT NULL" +
            " GROUP BY launch_year, energy_name" +
            " ORDER BY launch_year, cnt DESC"
        );
    }

    public List<Map<String, Object>> newEnergyRatioByYear() throws SQLException {
        return query(
            "SELECT launch_year," +
            " COUNT(*) AS total," +
            " SUM(CASE WHEN energy_name IN ('纯电动','插电式混合动力','增程式','油电混合')" +
            "   THEN 1 ELSE 0 END) AS new_energy_cnt," +
            " ROUND(SUM(CASE WHEN energy_name IN ('纯电动','插电式混合动力','增程式','油电混合')" +
            "   THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS new_energy_pct" +
            " FROM cars" +
            " WHERE launch_year IS NOT NULL" +
            " GROUP BY launch_year" +
            " HAVING total >= 10" +
            " ORDER BY launch_year"
        );
    }

    public List<Map<String, Object>> energyTrendBySeason() throws SQLException {
        return query(
            "SELECT launch_year, launch_season, energy_name, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE launch_year IS NOT NULL AND launch_season IS NOT NULL AND energy_name IS NOT NULL" +
            " GROUP BY launch_year, launch_season, energy_name" +
            " ORDER BY launch_year, launch_season, cnt DESC"
        );
    }

    public List<Map<String, Object>> brandNewEnergyRatio() throws SQLException {
        return query(
            "SELECT brand," +
            " COUNT(*) AS total," +
            " SUM(CASE WHEN energy_name IN ('纯电动','插电式混合动力','增程式','油电混合')" +
            "   THEN 1 ELSE 0 END) AS new_energy_cnt," +
            " ROUND(SUM(CASE WHEN energy_name IN ('纯电动','插电式混合动力','增程式','油电混合')" +
            "   THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS new_energy_pct" +
            " FROM cars" +
            " GROUP BY brand" +
            " HAVING total >= 5" +
            " ORDER BY new_energy_pct DESC"
        );
    }

    // ==================== 3. 排量 × 燃油标号 × 马力 ====================

    public List<Map<String, Object>> displacementFuelMatch() throws SQLException {
        return query(
            "SELECT CASE" +
            "  WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END AS displacement_range," +
            " CASE WHEN CAST(fuel_grade AS INT) = 0 THEN '未知'" +
            "  ELSE CONCAT(CAST(fuel_grade AS STRING), '号') END AS fuel_label," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE displacement_L IS NOT NULL AND fuel_grade IS NOT NULL AND CAST(fuel_grade AS INT) > 0" +
            " GROUP BY" +
            "  CASE WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END," +
            "  CASE WHEN CAST(fuel_grade AS INT) = 0 THEN '未知'" +
            "  ELSE CONCAT(CAST(fuel_grade AS STRING), '号') END" +
            " ORDER BY displacement_range, cnt DESC"
        );
    }

    public List<Map<String, Object>> displacementHorsepowerCorrelation() throws SQLException {
        return query(
            "SELECT CASE" +
            "  WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END AS displacement_range," +
            " COUNT(*) AS cnt," +
            " ROUND(AVG(horsepower), 0) AS hp_avg," +
            " ROUND(MIN(horsepower), 0) AS hp_min," +
            " ROUND(MAX(horsepower), 0) AS hp_max" +
            " FROM cars" +
            " WHERE displacement_L IS NOT NULL AND horsepower IS NOT NULL" +
            " GROUP BY" +
            "  CASE WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END" +
            " ORDER BY hp_avg"
        );
    }

    public List<Map<String, Object>> displacementFuelHorsepowerDetail() throws SQLException {
        return query(
            "SELECT displacement_L," +
            " CASE WHEN CAST(fuel_grade AS INT) = 0 THEN '未知'" +
            "  ELSE CONCAT(CAST(fuel_grade AS STRING), '号') END AS fuel_label," +
            " ROUND(AVG(horsepower), 0) AS hp_avg," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE displacement_L IS NOT NULL AND horsepower IS NOT NULL AND CAST(fuel_grade AS INT) > 0" +
            " GROUP BY displacement_L, fuel_grade" +
            " HAVING cnt >= 3" +
            " ORDER BY displacement_L, fuel_grade"
        );
    }

    // ==================== 4. 车身结构 × 座位数 × 级别 ====================

    public List<Map<String, Object>> levelSeatsDistribution() throws SQLException {
        return query(
            "SELECT level, seats, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE level IS NOT NULL AND seats IS NOT NULL" +
            " GROUP BY level, seats" +
            " ORDER BY level, seats"
        );
    }

    public List<Map<String, Object>> bodyTypeSeatsDistribution() throws SQLException {
        return query(
            "SELECT body_type, seats, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE body_type IS NOT NULL AND seats IS NOT NULL" +
            " GROUP BY body_type, seats" +
            " ORDER BY body_type, seats"
        );
    }

    public List<Map<String, Object>> levelBodyTypeSeats(String level) throws SQLException {
        String safeLevel = level.replace("'", "''");
        return query(
            "SELECT body_type, seats, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE level = '" + safeLevel + "' AND body_type IS NOT NULL AND seats IS NOT NULL" +
            " GROUP BY body_type, seats" +
            " ORDER BY cnt DESC"
        );
    }

    public List<Map<String, Object>> levelSevenSeatRatio() throws SQLException {
        return query(
            "SELECT level," +
            " COUNT(*) AS total," +
            " SUM(CASE WHEN seats >= 7 THEN 1 ELSE 0 END) AS seven_seat_cnt," +
            " ROUND(SUM(CASE WHEN seats >= 7 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS seven_seat_pct" +
            " FROM cars" +
            " WHERE level IS NOT NULL AND seats IS NOT NULL" +
            " GROUP BY level" +
            " HAVING total >= 5" +
            " ORDER BY seven_seat_pct DESC"
        );
    }

    // ==================== 5. 变速箱 × 发动机配置 ====================

    public List<Map<String, Object>> transEngineCombo() throws SQLException {
        return query(
            "SELECT trans_type, engine_type, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE trans_type IS NOT NULL AND engine_type IS NOT NULL" +
            " GROUP BY trans_type, engine_type" +
            " ORDER BY trans_type, cnt DESC"
        );
    }

    public List<Map<String, Object>> displacementTransTypeDistribution() throws SQLException {
        return query(
            "SELECT CASE" +
            "  WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END AS displacement_range," +
            " trans_type, COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE displacement_L IS NOT NULL AND trans_type IS NOT NULL" +
            " GROUP BY" +
            "  CASE WHEN displacement_L <= 1.2 THEN '1.2L及以下'" +
            "  WHEN displacement_L <= 1.6 THEN '1.2-1.6L'" +
            "  WHEN displacement_L <= 2.0 THEN '1.6-2.0L'" +
            "  WHEN displacement_L <= 3.0 THEN '2.0-3.0L'" +
            "  ELSE '3.0L以上' END," +
            "  trans_type" +
            " ORDER BY displacement_range, cnt DESC"
        );
    }

    public List<Map<String, Object>> transTypeDisplacementHorsepower() throws SQLException {
        return query(
            "SELECT trans_type," +
            " COUNT(*) AS cnt," +
            " ROUND(AVG(displacement_L), 2) AS disp_avg," +
            " ROUND(AVG(horsepower), 0) AS hp_avg" +
            " FROM cars" +
            " WHERE trans_type IS NOT NULL AND displacement_L IS NOT NULL AND horsepower IS NOT NULL" +
            " GROUP BY trans_type" +
            " ORDER BY disp_avg"
        );
    }

    public List<Map<String, Object>> transTypeElectricCombo() throws SQLException {
        return query(
            "SELECT trans_type," +
            " CASE WHEN is_electric = 1 THEN '电动' ELSE '燃油' END AS power_type," +
            " COUNT(*) AS cnt" +
            " FROM cars" +
            " WHERE trans_type IS NOT NULL" +
            " GROUP BY trans_type, is_electric" +
            " ORDER BY trans_type, cnt DESC"
        );
    }
}
