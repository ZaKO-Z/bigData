package com.example.cars;

import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 业务关联分析 API
 *
 * 基础路径: /api/cars/biz
 *
 * 1. 品牌 × 级别 × 指导价       /brand-level-price/**
 * 2. 上市时间 × 能源类型         /energy-trend/**
 * 3. 发动机排量 × 燃油标号 × 马力 /power-match/**
 * 4. 车身结构 × 座位数 × 级别     /space-config/**
 * 5. 变速箱类型 × 发动机配置      /powertrain/**
 */
@RestController
@RequestMapping("/api/cars/biz")
public class BusinessAnalysisController {

    private final BusinessAnalysisService service;

    public BusinessAnalysisController(BusinessAnalysisService service) {
        this.service = service;
    }

    // ==================== 1. 品牌 × 级别 × 指导价 ====================

    /** 各品牌在各级别中的价格区间（最低价、最高价、均价、车型数） */
    @GetMapping("/brand-level-price/band")
    public List<Map<String, Object>> brandLevelPriceBand() throws SQLException {
        return service.brandLevelPriceBand();
    }

    /** 指定级别下各品牌定价对比 */
    @GetMapping("/brand-level-price/by-level")
    public List<Map<String, Object>> brandPriceByLevel(
            @RequestParam String level) throws SQLException {
        return service.brandPriceByLevel(level);
    }

    /** 各级别的品牌数量与价格跨度总览 */
    @GetMapping("/brand-level-price/level-summary")
    public List<Map<String, Object>> levelBrandSummary() throws SQLException {
        return service.levelBrandSummary();
    }

    // ==================== 2. 上市时间 × 能源类型 ====================

    /** 按年份统计各能源类型新车数量 */
    @GetMapping("/energy-trend/by-year")
    public List<Map<String, Object>> energyTrendByYear() throws SQLException {
        return service.energyTrendByYear();
    }

    /** 按年份统计新能源 vs 燃油车占比 */
    @GetMapping("/energy-trend/new-energy-ratio")
    public List<Map<String, Object>> newEnergyRatioByYear() throws SQLException {
        return service.newEnergyRatioByYear();
    }

    /** 按季度统计各能源类型新车数量 */
    @GetMapping("/energy-trend/by-season")
    public List<Map<String, Object>> energyTrendBySeason() throws SQLException {
        return service.energyTrendBySeason();
    }

    /** 各品牌的新能源车型占比 */
    @GetMapping("/energy-trend/brand-ratio")
    public List<Map<String, Object>> brandNewEnergyRatio() throws SQLException {
        return service.brandNewEnergyRatio();
    }

    // ==================== 3. 发动机排量 × 燃油标号 × 马力 ====================

    /** 各排量段的主流燃油标号分布 */
    @GetMapping("/power-match/displacement-fuel")
    public List<Map<String, Object>> displacementFuelMatch() throws SQLException {
        return service.displacementFuelMatch();
    }

    /** 各排量段的平均马力与马力区间 */
    @GetMapping("/power-match/displacement-horsepower")
    public List<Map<String, Object>> displacementHorsepowerCorrelation() throws SQLException {
        return service.displacementHorsepowerCorrelation();
    }

    /** 排量×燃油标号×马力的详细交叉表 */
    @GetMapping("/power-match/detail")
    public List<Map<String, Object>> displacementFuelHorsepowerDetail() throws SQLException {
        return service.displacementFuelHorsepowerDetail();
    }

    // ==================== 4. 车身结构 × 座位数 × 级别 ====================

    /** 各级别的座位数分布 */
    @GetMapping("/space-config/level-seats")
    public List<Map<String, Object>> levelSeatsDistribution() throws SQLException {
        return service.levelSeatsDistribution();
    }

    /** 各车身结构的座位数分布 */
    @GetMapping("/space-config/body-type-seats")
    public List<Map<String, Object>> bodyTypeSeatsDistribution() throws SQLException {
        return service.bodyTypeSeatsDistribution();
    }

    /** 指定级别下车身结构与座位数的交叉分布 */
    @GetMapping("/space-config/level-body-seats")
    public List<Map<String, Object>> levelBodyTypeSeats(
            @RequestParam String level) throws SQLException {
        return service.levelBodyTypeSeats(level);
    }

    /** 各级别7座车型占比 */
    @GetMapping("/space-config/seven-seat-ratio")
    public List<Map<String, Object>> levelSevenSeatRatio() throws SQLException {
        return service.levelSevenSeatRatio();
    }

    // ==================== 5. 变速箱类型 × 发动机配置 ====================

    /** 变速箱类型 × 发动机类型的搭配频次 */
    @GetMapping("/powertrain/trans-engine-combo")
    public List<Map<String, Object>> transEngineCombo() throws SQLException {
        return service.transEngineCombo();
    }

    /** 各排量段的主流变速箱类型分布 */
    @GetMapping("/powertrain/displacement-trans")
    public List<Map<String, Object>> displacementTransTypeDistribution() throws SQLException {
        return service.displacementTransTypeDistribution();
    }

    /** 各变速箱类型下的平均排量与平均马力 */
    @GetMapping("/powertrain/trans-disp-hp")
    public List<Map<String, Object>> transTypeDisplacementHorsepower() throws SQLException {
        return service.transTypeDisplacementHorsepower();
    }

    /** 变速箱类型 × 是否电动 的搭配统计 */
    @GetMapping("/powertrain/trans-electric")
    public List<Map<String, Object>> transTypeElectricCombo() throws SQLException {
        return service.transTypeElectricCombo();
    }
}
