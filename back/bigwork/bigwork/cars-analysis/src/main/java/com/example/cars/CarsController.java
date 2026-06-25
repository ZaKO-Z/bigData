package com.example.cars;

import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
public class CarsController {

    private final CarsService service;
    private final HiveConfig.ConnectionProvider connectionProvider;

    public CarsController(CarsService service, HiveConfig.ConnectionProvider connectionProvider) {
        this.service = service;
        this.connectionProvider = connectionProvider;
    }

    @GetMapping("/datasource")
    public Map<String, String> dataSource() {
        Map<String, String> info = new java.util.LinkedHashMap<>();
        String source = connectionProvider.getActiveSource();
        info.put("source", source);
        info.put("label", "hive".equals(source) ? "Hive 数据仓库" : "H2 本地数据库");
        info.put("status", "hive".equals(source) ? "connected" : "fallback");
        return info;
    }

    @GetMapping("/price-distribution")
    public List<Map<String, Object>> priceDistribution() throws SQLException {
        return service.priceDistribution();
    }

    @GetMapping("/brand-ranking")
    public List<Map<String, Object>> brandRanking() throws SQLException {
        return service.brandRanking();
    }

    @GetMapping("/energy-distribution")
    public List<Map<String, Object>> energyDistribution() throws SQLException {
        return service.energyDistribution();
    }

    @GetMapping("/level-distribution")
    public List<Map<String, Object>> levelDistribution() throws SQLException {
        return service.levelDistribution();
    }

    /** 仪表盘概览：总车数、品牌数、均价、最贵/最便宜、Top10品牌、价格分布、能源分布 */
    @GetMapping("/overview")
    public Map<String, Object> overview() throws SQLException {
        return service.overview();
    }

    /** 车辆搜索：支持品牌/级别/价格区间/能源类型/变速箱 */
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String energy,
            @RequestParam(required = false) String transType,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) throws SQLException {
        List<Map<String, Object>> data = service.search(brand, level, minPrice, maxPrice, energy, transType, limit, offset);
        long total = service.searchCount(brand, level, minPrice, maxPrice, energy, transType);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", total);
        result.put("data", data);
        return result;
    }

    /** 获取所有品牌列表 */
    @GetMapping("/brands")
    public List<Map<String, Object>> brands() throws SQLException {
        return service.brandList();
    }

    /** 获取所有级别列表 */
    @GetMapping("/levels")
    public List<Map<String, Object>> levels() throws SQLException {
        return service.levelList();
    }

    /** 品牌对比 */
    @GetMapping("/compare/brands")
    public Map<String, Object> compareBrands(
            @RequestParam String brand1, @RequestParam String brand2) throws SQLException {
        if (brand1 == null || brand1.isEmpty() || brand2 == null || brand2.isEmpty()) {
            throw new IllegalArgumentException("品牌名称不能为空");
        }
        return service.compareBrands(brand1, brand2);
    }

    /** 级别对比 */
    @GetMapping("/compare/levels")
    public Map<String, Object> compareLevels(
            @RequestParam String level1, @RequestParam String level2) throws SQLException {
        if (level1 == null || level1.isEmpty() || level2 == null || level2.isEmpty()) {
            throw new IllegalArgumentException("级别名称不能为空");
        }
        return service.compareLevels(level1, level2);
    }
}
