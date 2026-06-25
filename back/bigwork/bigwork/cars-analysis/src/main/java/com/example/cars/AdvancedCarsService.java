package com.example.cars;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class AdvancedCarsService {

    private final HiveConfig.ConnectionProvider connectionProvider;

    public AdvancedCarsService(HiveConfig.ConnectionProvider connectionProvider) {
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

    // ==================== 分类维度 ====================

    public List<Map<String, Object>> groupByBrand() throws SQLException {
        return query("SELECT brand, COUNT(*) AS cnt FROM cars GROUP BY brand ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByLevel() throws SQLException {
        return query("SELECT level, COUNT(*) AS cnt FROM cars GROUP BY level ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByEnergy() throws SQLException {
        return query("SELECT energy_name, COUNT(*) AS cnt FROM cars GROUP BY energy_name ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByBodyType() throws SQLException {
        return query("SELECT body_type, COUNT(*) AS cnt FROM cars WHERE body_type IS NOT NULL GROUP BY body_type ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByTransType() throws SQLException {
        return query("SELECT trans_type, COUNT(*) AS cnt FROM cars WHERE trans_type IS NOT NULL GROUP BY trans_type ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByTransSubtype() throws SQLException {
        return query(
            "SELECT CONCAT(COALESCE(trans_type,'未知'), '-', COALESCE(CAST(gear_count AS STRING),'?'), '挡') AS trans_subtype," +
            " COUNT(*) AS cnt FROM cars" +
            " GROUP BY trans_type, gear_count ORDER BY cnt DESC"
        );
    }

    public List<Map<String, Object>> groupByFuelGrade() throws SQLException {
        return query(
            "SELECT CASE WHEN fuel_grade = 0 THEN '未知'" +
            " ELSE CONCAT(CAST(fuel_grade AS STRING), '号') END AS fuel_label," +
            " COUNT(*) AS cnt FROM cars" +
            " GROUP BY fuel_grade ORDER BY cnt DESC"
        );
    }

    public List<Map<String, Object>> groupByEngineLayout() throws SQLException {
        return query("SELECT engine_type, COUNT(*) AS cnt FROM cars WHERE engine_type IS NOT NULL GROUP BY engine_type ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> groupByWarrantyUnlimited() throws SQLException {
        return query(
            "SELECT CASE WHEN warranty_km_wan = -1 THEN '不限里程'" +
            " WHEN warranty_km_wan IS NULL THEN '未知'" +
            " ELSE '有限里程' END AS warranty_km_type," +
            " COUNT(*) AS cnt FROM cars" +
            " GROUP BY CASE WHEN warranty_km_wan = -1 THEN '不限里程'" +
            " WHEN warranty_km_wan IS NULL THEN '未知'" +
            " ELSE '有限里程' END ORDER BY cnt DESC"
        );
    }

    // ==================== 离散化分类 ====================

    public List<Map<String, Object>> groupByLaunchYear() throws SQLException {
        return query("SELECT launch_year, COUNT(*) AS cnt FROM cars WHERE launch_year IS NOT NULL GROUP BY launch_year ORDER BY launch_year");
    }

    public List<Map<String, Object>> groupByPriceRange() throws SQLException {
        return query(
            "SELECT CASE" +
            "  WHEN price_wan < 10 THEN '10万以下'" +
            "  WHEN price_wan < 20 THEN '10-20万'" +
            "  WHEN price_wan < 30 THEN '20-30万'" +
            "  WHEN price_wan < 50 THEN '30-50万'" +
            "  ELSE '50万以上' END AS price_range," +
            " COUNT(*) AS cnt FROM cars" +
            " WHERE price_wan IS NOT NULL" +
            " GROUP BY CASE" +
            "  WHEN price_wan < 10 THEN '10万以下'" +
            "  WHEN price_wan < 20 THEN '10-20万'" +
            "  WHEN price_wan < 30 THEN '20-30万'" +
            "  WHEN price_wan < 50 THEN '30-50万'" +
            "  ELSE '50万以上' END"
        );
    }

    public List<Map<String, Object>> groupBySeats() throws SQLException {
        return query("SELECT seats, COUNT(*) AS cnt FROM cars WHERE seats IS NOT NULL GROUP BY seats ORDER BY seats");
    }

    public List<Map<String, Object>> groupByGearCount() throws SQLException {
        return query("SELECT gear_count, COUNT(*) AS cnt FROM cars WHERE gear_count IS NOT NULL GROUP BY gear_count ORDER BY gear_count");
    }

    // ==================== 排序度量 ====================

    public List<Map<String, Object>> sortByLaunchDate(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, level, energy_name, launch_date, price_wan FROM cars WHERE launch_date IS NOT NULL ORDER BY launch_date " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByPrice(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, level, energy_name, price_wan, launch_date FROM cars WHERE price_wan IS NOT NULL ORDER BY price_wan " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByDisplacement(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, level, displacement_L, horsepower, engine_type FROM cars WHERE displacement_L IS NOT NULL ORDER BY displacement_L " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByHorsepower(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, level, horsepower, displacement_L, engine_type FROM cars WHERE horsepower IS NOT NULL ORDER BY horsepower " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByDoors(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, body_type, doors, seats FROM cars WHERE doors IS NOT NULL ORDER BY doors " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortBySeats(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, body_type, seats, doors FROM cars WHERE seats IS NOT NULL ORDER BY seats " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByGearCount(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, trans_type, gear_count FROM cars WHERE gear_count IS NOT NULL ORDER BY gear_count " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByWarrantyYears(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, warranty_years, warranty_km_wan FROM cars WHERE warranty_years IS NOT NULL ORDER BY warranty_years " + dir + " LIMIT " + lim);
    }

    public List<Map<String, Object>> sortByWarrantyKm(String order, int limit) throws SQLException {
        String dir = safeOrder(order);
        int lim = safeLimit(limit);
        return query("SELECT brand, warranty_km_wan, warranty_years FROM cars WHERE warranty_km_wan IS NOT NULL ORDER BY warranty_km_wan " + dir + " LIMIT " + lim);
    }

    private String safeOrder(String order) {
        return "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
