package com.example.cars;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

/**
 * 文件数据服务：支持 CSV / Excel 文件上传、解析、导入 H2、预览
 */
@Service
public class FileDataService {

    private static final Logger log = LoggerFactory.getLogger(FileDataService.class);

    /**
     * 解析上传文件（CSV 或 Excel），返回表头和数据行
     */
    public ParsedFile parseFile(MultipartFile file) throws IOException, CsvException {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("文件名不能为空");

        if (filename.toLowerCase().endsWith(".csv")) {
            return parseCsv(file);
        } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 CSV 和 Excel（xlsx/xls）");
        }
    }

    private ParsedFile parseCsv(MultipartFile file) throws IOException, CsvException {
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            rows = reader.readAll();
        }
        if (rows.isEmpty()) return new ParsedFile(Collections.emptyList(), Collections.emptyList());

        String[] headers = rows.get(0);
        List<Map<String, String>> dataRows = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], j < cols.length ? cols[j] : "");
            }
            dataRows.add(row);
        }
        return new ParsedFile(Arrays.asList(headers), dataRows);
    }

    private ParsedFile parseExcel(MultipartFile file) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> dataRows = new ArrayList<>();

        Iterator<Row> rowIter = sheet.iterator();
        if (rowIter.hasNext()) {
            Row headerRow = rowIter.next();
            for (Cell cell : headerRow) {
                headers.add(getCellStringValue(cell));
            }
        }

        while (rowIter.hasNext()) {
            Row row = rowIter.next();
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                rowMap.put(headers.get(j), getCellStringValue(cell));
            }
            dataRows.add(rowMap);
        }
        workbook.close();
        return new ParsedFile(headers, dataRows);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
                return String.valueOf(v);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    /**
     * 将解析后的数据导入到 H2 的 cars 表（覆盖原有数据）
     */
    public int importToCarsTable(ParsedFile parsed) throws SQLException {
        List<String> headers = parsed.headers;
        List<Map<String, String>> rows = parsed.rows;

        // 查找 cars 表需要的列映射
        String[] carCols = {"brand", "level", "energy_code", "energy_name", "fuel_grade",
            "launch_date", "launch_year", "launch_month", "launch_season", "price_wan",
            "warranty_years", "warranty_km_wan", "displacement_L", "horsepower", "engine_type",
            "is_electric", "gear_count", "trans_type", "doors", "seats", "body_type", "body_structure"};

        // 检查上传文件的列是否匹配 cars 表
        Set<String> headerSet = new HashSet<>(headers);
        boolean matchesCars = true;
        for (String col : carCols) {
            if (!headerSet.contains(col)) { matchesCars = false; break; }
        }

        if (!matchesCars) {
            // 不匹配 cars 表结构，存入通用表
            return importToGenericTable(headers, rows);
        }

        // 匹配 cars 表，清空后导入
        try (Connection conn = DataSourceConfig.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM cars");
            }
            String placeholders = String.join(",", Collections.nCopies(carCols.length, "?"));
            String insertSql = "INSERT INTO cars (" + String.join(",", carCols) + ") VALUES (" + placeholders + ")";
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int count = 0;
                for (Map<String, String> row : rows) {
                    for (int j = 0; j < carCols.length; j++) {
                        String val = row.getOrDefault(carCols[j], "");
                        if (val.isEmpty()) {
                            ps.setNull(j + 1, Types.VARCHAR);
                        } else {
                            ps.setString(j + 1, val);
                        }
                    }
                    ps.addBatch();
                    count++;
                    if (count % 5000 == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
                log.info("导入 cars 表 {} 条数据", count);
                return count;
            }
        }
    }

    private int importToGenericTable(List<String> headers, List<Map<String, String>> rows) throws SQLException {
        try (Connection conn = DataSourceConfig.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS generic_data");
                StringBuilder createSql = new StringBuilder("CREATE TABLE generic_data (id INT AUTO_INCREMENT PRIMARY KEY");
                for (String h : headers) {
                    createSql.append(", \"").append(h.replace("\"", "\"\"")).append("\" VARCHAR(500)");
                }
                createSql.append(")");
                stmt.execute(createSql.toString());
            }

            String colNames = headers.stream().map(h -> "\"" + h.replace("\"", "\"\"") + "\"").reduce((a, b) -> a + "," + b).orElse("");
            String placeholders = headers.stream().map(h -> "?").reduce((a, b) -> a + "," + b).orElse("");
            String insertSql = "INSERT INTO generic_data (" + colNames + ") VALUES (" + placeholders + ")";

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int count = 0;
                for (Map<String, String> row : rows) {
                    for (int j = 0; j < headers.size(); j++) {
                        String val = row.getOrDefault(headers.get(j), "");
                        ps.setString(j + 1, val.isEmpty() ? null : val);
                    }
                    ps.addBatch();
                    count++;
                    if (count % 5000 == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
                log.info("导入 generic_data 表 {} 条数据", count);
                return count;
            }
        }
    }

    /**
     * 获取数据预览（前 N 行）
     */
    public List<Map<String, Object>> previewData(String tableName, int limit) throws SQLException {
        String safeTable = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        try (Connection conn = DataSourceConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + safeTable + " LIMIT " + limit)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    /**
     * 获取表结构信息
     */
    public List<Map<String, Object>> getTableInfo(String tableName) throws SQLException {
        String safeTable = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        try (Connection conn = DataSourceConfig.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<Map<String, Object>> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(null, null, safeTable.toUpperCase(), null)) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("column_name", rs.getString("COLUMN_NAME"));
                    col.put("type", rs.getString("TYPE_NAME"));
                    columns.add(col);
                }
            }
            return columns;
        }
    }

    /**
     * 解析后的文件数据
     */
    public static class ParsedFile {
        public final List<String> headers;
        public final List<Map<String, String>> rows;

        public ParsedFile(List<String> headers, List<Map<String, String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }
}
