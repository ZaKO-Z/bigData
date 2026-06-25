package com.example.cars;

import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 高级数据分析 API
 *
 * 分类维度:  /api/cars/advanced/group/{dimension}
 * 排序度量:  /api/cars/advanced/sort/{metric}?order=desc&limit=10
 */
@RestController
@RequestMapping("/api/cars/advanced")
public class AdvancedCarsController {

    private final AdvancedCarsService service;

    public AdvancedCarsController(AdvancedCarsService service) {
        this.service = service;
    }

    // ==================== 分类维度 ====================

    /** 1. 按品牌分类 */
    @GetMapping("/group/brand")
    public List<Map<String, Object>> groupByBrand() throws SQLException {
        return service.groupByBrand();
    }

    /** 2. 按级别分类 */
    @GetMapping("/group/level")
    public List<Map<String, Object>> groupByLevel() throws SQLException {
        return service.groupByLevel();
    }

    /** 3. 按能源类型分类 */
    @GetMapping("/group/energy")
    public List<Map<String, Object>> groupByEnergy() throws SQLException {
        return service.groupByEnergy();
    }

    /** 4. 按车身结构分类 */
    @GetMapping("/group/body-type")
    public List<Map<String, Object>> groupByBodyType() throws SQLException {
        return service.groupByBodyType();
    }

    /** 5. 按变速箱大类分类 */
    @GetMapping("/group/trans-type")
    public List<Map<String, Object>> groupByTransType() throws SQLException {
        return service.groupByTransType();
    }

    /** 6. 按变速箱亚型分类（大类+档位数） */
    @GetMapping("/group/trans-subtype")
    public List<Map<String, Object>> groupByTransSubtype() throws SQLException {
        return service.groupByTransSubtype();
    }

    /** 7. 按燃油标号分类 */
    @GetMapping("/group/fuel-grade")
    public List<Map<String, Object>> groupByFuelGrade() throws SQLException {
        return service.groupByFuelGrade();
    }

    /** 8. 按发动机气缸布局分类 */
    @GetMapping("/group/engine-layout")
    public List<Map<String, Object>> groupByEngineLayout() throws SQLException {
        return service.groupByEngineLayout();
    }

    /** 9. 按质保里程是否不限分类 */
    @GetMapping("/group/warranty-unlimited")
    public List<Map<String, Object>> groupByWarrantyUnlimited() throws SQLException {
        return service.groupByWarrantyUnlimited();
    }

    // ==================== 离散化分类 ====================

    /** 10. 按上市年份分类 */
    @GetMapping("/group/launch-year")
    public List<Map<String, Object>> groupByLaunchYear() throws SQLException {
        return service.groupByLaunchYear();
    }

    /** 11. 按价格区间分类 */
    @GetMapping("/group/price-range")
    public List<Map<String, Object>> groupByPriceRange() throws SQLException {
        return service.groupByPriceRange();
    }

    /** 12. 按座位数分类 */
    @GetMapping("/group/seats")
    public List<Map<String, Object>> groupBySeats() throws SQLException {
        return service.groupBySeats();
    }

    /** 13. 按档位数分类 */
    @GetMapping("/group/gear-count")
    public List<Map<String, Object>> groupByGearCount() throws SQLException {
        return service.groupByGearCount();
    }

    // ==================== 排序度量 ====================

    /** 14. 按上市时间排序 */
    @GetMapping("/sort/launch-date")
    public List<Map<String, Object>> sortByLaunchDate(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByLaunchDate(order, limit);
    }

    /** 15. 按厂商指导价排序 */
    @GetMapping("/sort/price")
    public List<Map<String, Object>> sortByPrice(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByPrice(order, limit);
    }

    /** 16. 按发动机排量排序 */
    @GetMapping("/sort/displacement")
    public List<Map<String, Object>> sortByDisplacement(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByDisplacement(order, limit);
    }

    /** 17. 按最大马力排序 */
    @GetMapping("/sort/horsepower")
    public List<Map<String, Object>> sortByHorsepower(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByHorsepower(order, limit);
    }

    /** 18. 按门数排序 */
    @GetMapping("/sort/doors")
    public List<Map<String, Object>> sortByDoors(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByDoors(order, limit);
    }

    /** 19. 按座位数排序 */
    @GetMapping("/sort/seats")
    public List<Map<String, Object>> sortBySeats(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortBySeats(order, limit);
    }

    /** 20. 按变速箱档位数排序 */
    @GetMapping("/sort/gear-count")
    public List<Map<String, Object>> sortByGearCount(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByGearCount(order, limit);
    }

    /** 21. 按质保年限排序 */
    @GetMapping("/sort/warranty-years")
    public List<Map<String, Object>> sortByWarrantyYears(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByWarrantyYears(order, limit);
    }

    /** 22. 按质保里程排序 */
    @GetMapping("/sort/warranty-km")
    public List<Map<String, Object>> sortByWarrantyKm(
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        return service.sortByWarrantyKm(order, limit);
    }
}
