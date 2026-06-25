package com.example.cars;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class CarsService {

    private final HiveConfig.ConnectionProvider connectionProvider;

    public CarsService(HiveConfig.ConnectionProvider connectionProvider) {
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

    public List<Map<String, Object>> priceDistribution() throws SQLException {
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

    public List<Map<String, Object>> brandRanking() throws SQLException {
        return query("SELECT brand, COUNT(*) AS cnt FROM cars GROUP BY brand ORDER BY cnt DESC LIMIT 10");
    }

    public List<Map<String, Object>> energyDistribution() throws SQLException {
        return query("SELECT energy_name, COUNT(*) AS cnt FROM cars GROUP BY energy_name");
    }

    public List<Map<String, Object>> levelDistribution() throws SQLException {
        return query("SELECT level, COUNT(*) AS cnt FROM cars GROUP BY level ORDER BY cnt DESC");
    }

    /** 仪表盘概览：总车数、品牌数、均价、最贵、最便宜、级别分布等 */
    public Map<String, Object> overview() throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> stats = query(
            "SELECT COUNT(*) AS total, COUNT(DISTINCT brand) AS brands," +
            " ROUND(AVG(price_wan), 2) AS avg_price," +
            " MAX(price_wan) AS max_price, MIN(price_wan) AS min_price" +
            " FROM cars WHERE price_wan IS NOT NULL"
        );
        if (!stats.isEmpty()) result.putAll(stats.get(0));

        result.put("topBrands", brandRanking());
        result.put("priceDistribution", priceDistribution());
        result.put("energyDistribution", energyDistribution());

        return result;
    }

    /** 搜索筛选：支持品牌、级别、价格区间、能源类型、变速箱类型 */
    public List<Map<String, Object>> search(String brand, String level, Double minPrice,
            Double maxPrice, String energy, String transType, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM cars WHERE 1=1");
        if (brand != null && !brand.isEmpty()) {
            sql.append(" AND brand = '").append(brand.replace("'", "''")).append("'");
        }
        if (level != null && !level.isEmpty()) {
            sql.append(" AND level = '").append(level.replace("'", "''")).append("'");
        }
        if (minPrice != null) {
            sql.append(" AND price_wan >= ").append(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price_wan <= ").append(maxPrice);
        }
        if (energy != null && !energy.isEmpty()) {
            sql.append(" AND energy_name = '").append(energy.replace("'", "''")).append("'");
        }
        if (transType != null && !transType.isEmpty()) {
            sql.append(" AND trans_type = '").append(transType.replace("'", "''")).append("'");
        }
        // Hive 不支持 OFFSET 语法，改为多取 offset 条后在 Java 层跳过
        int fetchSize = limit + offset;
        sql.append(" LIMIT ").append(fetchSize);
        List<Map<String, Object>> rows = query(sql.toString());
        if (offset > 0 && rows.size() > offset) {
            return new ArrayList<>(rows.subList(offset, rows.size()));
        }
        return offset > 0 ? new ArrayList<>() : rows;
    }

    /** 搜索计数 */
    public long searchCount(String brand, String level, Double minPrice,
            Double maxPrice, String energy, String transType) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM cars WHERE 1=1");
        if (brand != null && !brand.isEmpty()) {
            sql.append(" AND brand = '").append(brand.replace("'", "''")).append("'");
        }
        if (level != null && !level.isEmpty()) {
            sql.append(" AND level = '").append(level.replace("'", "''")).append("'");
        }
        if (minPrice != null) {
            sql.append(" AND price_wan >= ").append(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price_wan <= ").append(maxPrice);
        }
        if (energy != null && !energy.isEmpty()) {
            sql.append(" AND energy_name = '").append(energy.replace("'", "''")).append("'");
        }
        if (transType != null && !transType.isEmpty()) {
            sql.append(" AND trans_type = '").append(transType.replace("'", "''")).append("'");
        }
        List<Map<String, Object>> rows = query(sql.toString());
        return rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue();
    }

    /** 获取所有品牌列表（去重） */
    public List<Map<String, Object>> brandList() throws SQLException {
        return query("SELECT DISTINCT brand FROM cars ORDER BY brand");
    }

    /** 获取所有级别列表（去重） */
    public List<Map<String, Object>> levelList() throws SQLException {
        return query("SELECT DISTINCT level FROM cars WHERE level IS NOT NULL ORDER BY level");
    }

    /** 品牌对比：对比两个品牌的各项指标 */
    public Map<String, Object> compareBrands(String brand1, String brand2) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand1", brand1);
        result.put("brand2", brand2);

        List<Map<String, Object>> stats = query(
            "SELECT brand, COUNT(*) AS total, ROUND(AVG(price_wan),2) AS avg_price," +
            " MIN(price_wan) AS min_price, MAX(price_wan) AS max_price," +
            " ROUND(AVG(displacement_L),2) AS avg_displacement," +
            " ROUND(AVG(horsepower),1) AS avg_horsepower," +
            " ROUND(AVG(gear_count),1) AS avg_gear_count" +
            " FROM cars WHERE brand IN ('" + brand1.replace("'","''") + "','" + brand2.replace("'","''") + "')" +
            " GROUP BY brand"
        );
        result.put("stats", stats);

        List<Map<String, Object>> levelDist = query(
            "SELECT brand, level, COUNT(*) AS cnt FROM cars" +
            " WHERE brand IN ('" + brand1.replace("'","''") + "','" + brand2.replace("'","''") + "')" +
            " AND level IS NOT NULL GROUP BY brand, level"
        );
        result.put("levelDistribution", levelDist);

        List<Map<String, Object>> energyDist = query(
            "SELECT brand, energy_name, COUNT(*) AS cnt FROM cars" +
            " WHERE brand IN ('" + brand1.replace("'","''") + "','" + brand2.replace("'","''") + "')" +
            " GROUP BY brand, energy_name"
        );
        result.put("energyDistribution", energyDist);

        return result;
    }

    /** 级别对比：对比两个级别的各项指标 */
    public Map<String, Object> compareLevels(String level1, String level2) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level1", level1);
        result.put("level2", level2);

        List<Map<String, Object>> stats = query(
            "SELECT level, COUNT(*) AS total, COUNT(DISTINCT brand) AS brands," +
            " ROUND(AVG(price_wan),2) AS avg_price," +
            " MIN(price_wan) AS min_price, MAX(price_wan) AS max_price," +
            " ROUND(AVG(displacement_L),2) AS avg_displacement," +
            " ROUND(AVG(horsepower),1) AS avg_horsepower" +
            " FROM cars WHERE level IN ('" + level1.replace("'","''") + "','" + level2.replace("'","''") + "')" +
            " GROUP BY level"
        );
        result.put("stats", stats);

        List<Map<String, Object>> energyDist = query(
            "SELECT level, energy_name, COUNT(*) AS cnt FROM cars" +
            " WHERE level IN ('" + level1.replace("'","''") + "','" + level2.replace("'","''") + "')" +
            " GROUP BY level, energy_name"
        );
        result.put("energyDistribution", energyDist);

        return result;
    }
}
