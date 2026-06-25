package com.example.cars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 分析报表生成与存储服务
 * - 生成全维度分析报表（JSON + CSV）
 * - 存储报表到本地文件
 * - 导出报表下载
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final String REPORT_DIR = "reports";

    private final CarsService carsService;
    private final AdvancedCarsService advancedCarsService;
    private final BusinessAnalysisService businessAnalysisService;

    public ReportService(CarsService carsService, AdvancedCarsService advancedCarsService,
                         BusinessAnalysisService businessAnalysisService) {
        this.carsService = carsService;
        this.advancedCarsService = advancedCarsService;
        this.businessAnalysisService = businessAnalysisService;
    }

    /**
     * 生成全维度分析报表，返回报表内容 Map
     */
    public Map<String, Object> generateFullReport() throws SQLException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("report_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 基础统计
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("品牌Top10", carsService.brandRanking());
        basic.put("价格区间分布", carsService.priceDistribution());
        basic.put("能源类型分布", carsService.energyDistribution());
        basic.put("车型级别分布", carsService.levelDistribution());
        report.put("基础统计", basic);

        // 分类维度
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("品牌分布", advancedCarsService.groupByBrand());
        group.put("级别分布", advancedCarsService.groupByLevel());
        group.put("能源类型", advancedCarsService.groupByEnergy());
        group.put("车身结构", advancedCarsService.groupByBodyType());
        group.put("变速箱大类", advancedCarsService.groupByTransType());
        group.put("变速箱亚型", advancedCarsService.groupByTransSubtype());
        group.put("燃油标号", advancedCarsService.groupByFuelGrade());
        group.put("气缸布局", advancedCarsService.groupByEngineLayout());
        group.put("质保里程类型", advancedCarsService.groupByWarrantyUnlimited());
        group.put("上市年份", advancedCarsService.groupByLaunchYear());
        group.put("价格区间", advancedCarsService.groupByPriceRange());
        group.put("座位数", advancedCarsService.groupBySeats());
        group.put("档位数", advancedCarsService.groupByGearCount());
        report.put("分类维度", group);

        // 业务关联
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("品牌级别价格区间", businessAnalysisService.brandLevelPriceBand());
        biz.put("级别品牌总览", businessAnalysisService.levelBrandSummary());
        biz.put("年度能源结构", businessAnalysisService.energyTrendByYear());
        biz.put("新能源渗透率", businessAnalysisService.newEnergyRatioByYear());
        biz.put("品牌新能源占比", businessAnalysisService.brandNewEnergyRatio());
        biz.put("排量燃油标号", businessAnalysisService.displacementFuelMatch());
        biz.put("排量马力相关性", businessAnalysisService.displacementHorsepowerCorrelation());
        biz.put("级别座位数分布", businessAnalysisService.levelSeatsDistribution());
        biz.put("7座车型占比", businessAnalysisService.levelSevenSeatRatio());
        biz.put("变速箱发动机搭配", businessAnalysisService.transEngineCombo());
        biz.put("排量段变速箱分布", businessAnalysisService.displacementTransTypeDistribution());
        biz.put("变速箱平均排量马力", businessAnalysisService.transTypeDisplacementHorsepower());
        report.put("业务关联", biz);

        return report;
    }

    /**
     * 生成报表并保存为 CSV 文件
     */
    public String saveReportAsCsv() throws SQLException, IOException {
        Map<String, Object> report = generateFullReport();
        new File(REPORT_DIR).mkdirs();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filePath = REPORT_DIR + "/analysis_report_" + timestamp + ".csv";

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write("\uFEFF"); // BOM for Excel
            for (Map.Entry<String, Object> section : report.entrySet()) {
                if (section.getKey().equals("report_time")) continue;
                writer.write("\n=== " + section.getKey() + " ===\n");
                Map<String, Object> sectionData = (Map<String, Object>) section.getValue();
                for (Map.Entry<String, Object> entry : sectionData.entrySet()) {
                    writer.write("\n--- " + entry.getKey() + " ---\n");
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) entry.getValue();
                    if (rows.isEmpty()) {
                        writer.write("(无数据)\n");
                        continue;
                    }
                    // 表头
                    List<String> headers = new ArrayList<>(rows.get(0).keySet());
                    writer.write(String.join(",", headers) + "\n");
                    // 数据行
                    for (Map<String, Object> row : rows) {
                        List<String> vals = new ArrayList<>();
                        for (String h : headers) {
                            Object v = row.get(h);
                            String s = v == null ? "" : v.toString().replace(",", "，");
                            vals.add(s);
                        }
                        writer.write(String.join(",", vals) + "\n");
                    }
                }
            }
        }
        log.info("报表已保存: {}", filePath);
        return filePath;
    }

    /**
     * 获取已保存的报表列表
     */
    public List<Map<String, Object>> listReports() {
        File dir = new File(REPORT_DIR);
        if (!dir.exists()) return Collections.emptyList();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (files == null) return Collections.emptyList();
        List<Map<String, Object>> list = new ArrayList<>();
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : files) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("filename", f.getName());
            info.put("size", f.length());
            info.put("modified", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(f.lastModified()));
            list.add(info);
        }
        return list;
    }

    /**
     * 读取报表文件内容
     */
    public String readReport(String filename) throws IOException {
        String safeName = filename.replaceAll("[^a-zA-Z0-9_.-]", "");
        File file = new File(REPORT_DIR + "/" + safeName);
        if (!file.exists()) throw new FileNotFoundException("报表不存在: " + safeName);
        return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
