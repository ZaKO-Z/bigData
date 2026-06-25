package com.example.cars;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
 * 数据分析控制器：文件上传、数据预览、报表生成与下载
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private final FileDataService fileDataService;
    private final ReportService reportService;
    private final CarsService carsService;

    public DataController(FileDataService fileDataService, ReportService reportService, CarsService carsService) {
        this.fileDataService = fileDataService;
        this.reportService = reportService;
        this.carsService = carsService;
    }

    /**
     * 上传数据文件（CSV / Excel），解析并返回预览
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            FileDataService.ParsedFile parsed = fileDataService.parseFile(file);
            result.put("success", true);
            result.put("filename", file.getOriginalFilename());
            result.put("headers", parsed.headers);
            result.put("rowCount", parsed.rows.size());
            // 返回前 20 行预览
            result.put("preview", parsed.rows.subList(0, Math.min(20, parsed.rows.size())));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 确认导入上传的数据到数据库
     */
    @PostMapping("/import")
    public Map<String, Object> importData(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            FileDataService.ParsedFile parsed = fileDataService.parseFile(file);
            int count = fileDataService.importToCarsTable(parsed);
            result.put("success", true);
            result.put("importedCount", count);
            result.put("headers", parsed.headers);
            result.put("preview", parsed.rows.subList(0, Math.min(20, parsed.rows.size())));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 预览数据库中的数据
     */
    @GetMapping("/preview")
    public Map<String, Object> previewData(
            @RequestParam(defaultValue = "cars") String table,
            @RequestParam(defaultValue = "50") int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = fileDataService.previewData(table, limit);
            List<Map<String, Object>> columns = fileDataService.getTableInfo(table);
            result.put("success", true);
            result.put("table", table);
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("totalShown", rows.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 生成全维度分析报表（JSON 格式）
     */
    @GetMapping("/report/generate")
    public Map<String, Object> generateReport() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, Object> report = reportService.generateFullReport();
            result.put("success", true);
            result.put("report", report);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 生成报表并保存为 CSV 文件
     */
    @PostMapping("/report/save")
    public Map<String, Object> saveReport() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String filePath = reportService.saveReportAsCsv();
            result.put("success", true);
            result.put("filePath", filePath);
            result.put("message", "报表已保存");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 获取已保存的报表列表
     */
    @GetMapping("/report/list")
    public Map<String, Object> listReports() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("reports", reportService.listReports());
        return result;
    }

    /**
     * 下载报表文件
     */
    @GetMapping("/report/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam String filename) {
        try {
            String content = reportService.readReport(filename);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 导出搜索结果/数据为 CSV 文件
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String energy,
            @RequestParam(required = false) String transType) {
        try {
            List<Map<String, Object>> data = carsService.search(brand, level, minPrice, maxPrice, energy, transType, 5000, 0);
            StringWriter sw = new StringWriter();
            if (!data.isEmpty()) {
                List<String> keys = new ArrayList<>(data.get(0).keySet());
                sw.write(String.join(",", keys) + "\n");
                for (Map<String, Object> row : data) {
                    for (int i = 0; i < keys.size(); i++) {
                        if (i > 0) sw.write(",");
                        Object val = row.get(keys.get(i));
                        if (val != null) {
                            String v = val.toString().replace("\"", "\"\"");
                            if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
                                sw.write("\"" + v + "\"");
                            } else {
                                sw.write(v);
                            }
                        }
                    }
                    sw.write("\n");
                }
            }
            byte[] bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
            String filename = "car_data_export_" + System.currentTimeMillis() + ".csv";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
