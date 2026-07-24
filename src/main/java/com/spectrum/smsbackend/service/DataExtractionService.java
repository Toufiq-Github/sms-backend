package com.spectrum.smsbackend.service;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.spectrum.smsbackend.dto.DataRecordResponse;
import com.spectrum.smsbackend.model.DataRecord;
import com.spectrum.smsbackend.model.DatasetFile;
import com.spectrum.smsbackend.model.DatasetSchema;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.DataRecordRepository;
import com.spectrum.smsbackend.repository.DatasetFileRepository;
import com.spectrum.smsbackend.repository.DatasetSchemaRepository;
import com.spectrum.smsbackend.repository.UserRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataExtractionService {

    @Autowired
    private DatasetSchemaRepository datasetSchemaRepository;

    @Autowired
    private DatasetFileRepository datasetFileRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static class ExtractedContent {
        List<String> columns;
        List<Map<String, Object>> rows;
        ExtractedContent(List<String> columns,
                         List<Map<String, Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    // ==========================================
    // MAIN ENTRY POINT
    // ==========================================
    public DataRecordResponse extractAndSave(
            MultipartFile file,
            String dataCategory,
            String userEmail,
            String eodDate) throws IOException {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow();

        String fileName = normalizeFileName(
                file.getOriginalFilename());
        String extension = fileName.substring(
                        fileName.lastIndexOf(".") + 1)
                .toLowerCase();

        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        // ---- PDF goes through the SIMPLE,
        // legacy key-value path (stable, no
        // dynamic table creation for now) ----
        if (extension.equals("pdf")) {
            return extractPdfLegacy(
                    file, fileName, dataCategory,
                    user, year, month, day, eodDate);
        }

        // ---- Excel / CSV go through the
        // dynamic per-schema table pipeline ----
        ExtractedContent content;
        String fileType;

        switch (extension) {
            case "xlsx":
                content = parseExcel(file, false);
                fileType = "XLSX";
                break;
            case "xls":
                content = parseExcel(file, true);
                fileType = "XLS";
                break;
            case "csv":
                content = parseCsv(file);
                fileType = "CSV";
                break;
            default:
                throw new IOException(
                        "Unsupported file type: "
                                + extension);
        }

        if (content.columns.isEmpty() ||
                content.rows.isEmpty()) {
            DataRecordResponse empty =
                    new DataRecordResponse();
            empty.setSourceFileName(fileName);
            empty.setTotalRows(0);
            empty.setMessage(
                    "File parsed but contained no "
                            + "usable rows/columns.");
            return empty;
        }

        // Re-upload of the same filename by the
        // same user replaces old data completely
        datasetFileRepository
                .findByFileNameAndUploadedById(
                        fileName, user.getId())
                .ifPresent(this::removeDatasetFile);

        // getOrCreateSchema + insert must succeed
        // together, or the whole upload fails
        // cleanly with nothing half-saved
        DatasetSchema schema;
        DatasetFile datasetFile;
        try {
            schema = getOrCreateSchema(
                    dataCategory, content.columns);
            List<String> safeColumns =
                    getSafeColumnNames(
                            schema.getColumnDefinitions());

            datasetFile = new DatasetFile();
            datasetFile.setFileName(fileName);
            datasetFile.setFileType(fileType);
            datasetFile.setDataCategory(dataCategory);
            datasetFile.setSchema(schema);
            datasetFile.setTableName(
                    schema.getTableName());
            datasetFile.setTotalRows(
                    content.rows.size());
            datasetFile.setUploadedBy(user);
            datasetFile.setUploadYear(year);
            datasetFile.setUploadMonth(month);
            datasetFile.setUploadDay(day);
            datasetFile.setEodDate(eodDate);
            datasetFile.setCreatedAt(now);
            datasetFile = datasetFileRepository
                    .save(datasetFile);

            insertRowsIntoTable(
                    schema.getTableName(),
                    safeColumns,
                    content.columns,
                    datasetFile.getId(),
                    content.rows);

        } catch (Exception e) {
            // Roll back the DatasetFile row if the
            // insert step failed, so no orphan
            // registry entries are ever left behind
            throw new IOException(
                    "Failed to save extracted data: "
                            + e.getMessage(), e);
        }

        DataRecordResponse response =
                new DataRecordResponse();
        response.setSourceFileName(fileName);
        response.setSourceFileType(fileType);
        response.setDataCategory(dataCategory);
        response.setTotalRows(content.rows.size());
        response.setTotalColumns(
                content.columns.size());
        response.setColumns(content.columns);
        response.setRows(content.rows);
        response.setUploadedBy(user.getFullName());
        response.setUploadedAt(now.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm")));
        response.setMessage(
                fileType + " extracted and saved to table '"
                        + schema.getTableName() + "'! "
                        + content.rows.size()
                        + " rows processed.");
        return response;
    }

    // ==========================================
    // PDF — LEGACY SIMPLE PATH (data_records)
    // ==========================================
    @Transactional
    private DataRecordResponse extractPdfLegacy(
            MultipartFile file,
            String fileName,
            String dataCategory,
            User user,
            int year, int month, int day,
            String eodDate) throws IOException {

        // Re-upload of same filename by same user
        // replaces old data
        dataRecordRepository
                .deleteBySourceFileNameAndUploadedById(
                        fileName, user.getId());

        PdfReader reader = new PdfReader(
                file.getInputStream());
        StringBuilder fullText = new StringBuilder();
        int pages = reader.getNumberOfPages();
        for (int i = 1; i <= pages; i++) {
            fullText.append(
                    PdfTextExtractor.getTextFromPage(
                            reader, i));
            fullText.append("\n");
        }
        reader.close();

        String[] lines = fullText.toString()
                .split("\\r?\\n");
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows =
                new ArrayList<>();
        List<DataRecord> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int savedRows = 0;

        if (lines.length > 0) {
            String firstLine = lines[0].trim();

            if (firstLine.contains(",") ||
                    firstLine.contains("\t") ||
                    firstLine.contains("|")) {

                String delimiter =
                        firstLine.contains(",") ? ","
                                : firstLine.contains("\t")
                                  ? "\t" : "\\|";

                for (String h :
                        firstLine.split(delimiter)) {
                    columns.add(h.trim());
                }

                for (int i = 1;
                     i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;
                    String[] values =
                            line.split(delimiter);
                    Map<String, Object> row =
                            new LinkedHashMap<>();

                    for (int j = 0;
                         j < columns.size(); j++) {
                        String colName =
                                columns.get(j);
                        String rawValue =
                                j < values.length
                                        ? values[j].trim()
                                        : "";
                        row.put(colName,
                                detectAndConvert(
                                        rawValue));
                        batch.add(buildLegacyRecord(
                                dataCategory,
                                fileName, "PDF", i,
                                colName, rawValue,
                                user, now));
                    }
                    rows.add(row);
                    savedRows++;
                }
            } else {
                columns.add("Content");
                int rowNum = 0;
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    Map<String, Object> row =
                            new LinkedHashMap<>();
                    row.put("Content", line);
                    rows.add(row);
                    batch.add(buildLegacyRecord(
                            dataCategory, fileName,
                            "PDF", ++rowNum,
                            "Content", line,
                            user, now));
                    savedRows++;
                }
            }
        }

        dataRecordRepository.saveAll(batch);

        DataRecordResponse response =
                new DataRecordResponse();
        response.setSourceFileName(fileName);
        response.setSourceFileType("PDF");
        response.setDataCategory(dataCategory);
        response.setTotalRows(savedRows);
        response.setTotalColumns(columns.size());
        response.setColumns(columns);
        response.setRows(rows);
        response.setUploadedBy(user.getFullName());
        response.setUploadedAt(now.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm")));
        response.setMessage(
                "PDF extracted and saved! "
                        + savedRows
                        + " rows processed.");
        return response;
    }

    private DataRecord buildLegacyRecord(
            String dataCategory, String fileName,
            String fileType, int rowNum,
            String colName, String rawValue,
            User user, LocalDateTime now) {
        DataRecord record = new DataRecord();
        record.setDataCategory(dataCategory);
        record.setSourceFileName(fileName);
        record.setSourceFileType(fileType);
        record.setRowNumber(rowNum);
        record.setColumnName(colName);
        record.setValue(rawValue);
        record.setDataType(getDataType(rawValue));
        record.setUploadedBy(user);
        record.setCreatedAt(now);
        return record;
    }

    // ==========================================
    // EXCEL PARSER
    // ==========================================
    private ExtractedContent parseExcel(
            MultipartFile file,
            boolean isXls) throws IOException {

        Workbook workbook = isXls
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows =
                new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                columns.add(
                        getCellValueAsString(cell)
                                .trim());
            }
        }

        for (int i = 1;
             i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Object> rowMap =
                    new LinkedHashMap<>();
            boolean hasData = false;

            for (int j = 0;
                 j < columns.size(); j++) {
                Cell cell = row.getCell(j);
                String rawValue = cell != null
                        ? getCellValueAsString(cell)
                        : "";
                if (!rawValue.isEmpty()) hasData = true;
                rowMap.put(columns.get(j),
                        detectAndConvert(rawValue));
            }

            if (hasData) rows.add(rowMap);
        }

        workbook.close();
        return new ExtractedContent(columns, rows);
    }

    // ==========================================
    // CSV PARSER
    // ==========================================
    private ExtractedContent parseCsv(
            MultipartFile file) throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        file.getInputStream()));

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows =
                new ArrayList<>();
        String line;
        int lineNum = 0;

        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",", -1);

            if (lineNum == 0) {
                for (String v : values) {
                    columns.add(v.trim()
                            .replace("\"", ""));
                }
            } else {
                Map<String, Object> rowMap =
                        new LinkedHashMap<>();
                for (int j = 0;
                     j < columns.size(); j++) {
                    String rawValue =
                            j < values.length
                                    ? values[j].trim()
                                      .replace("\"", "")
                                    : "";
                    rowMap.put(columns.get(j),
                            detectAndConvert(rawValue));
                }
                rows.add(rowMap);
            }
            lineNum++;
        }
        reader.close();
        return new ExtractedContent(columns, rows);
    }

    // ==========================================
    // SCHEMA MATCHING / DYNAMIC TABLE CREATION
    // (Excel/CSV only)
    // ==========================================

    private DatasetSchema getOrCreateSchema(
            String category, List<String> columns) {

        String hash = computeSchemaHash(
                category, columns);

        Optional<DatasetSchema> existing =
                datasetSchemaRepository
                        .findBySchemaHash(hash);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<String> safeColumns =
                buildSafeColumnNames(columns);

        String categorySlug =
                sanitizeIdentifier(category, "cat");
        String tableName = "ds_" + categorySlug
                + "_" + hash.substring(0, 10);
        if (tableName.length() > 63) {
            tableName = tableName.substring(0, 63);
        }

        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ")
                .append(tableName).append(" (");
        ddl.append("id BIGSERIAL PRIMARY KEY, ");
        ddl.append("file_id BIGINT NOT NULL, ");
        ddl.append("row_number INTEGER, ");
        for (String col : safeColumns) {
            ddl.append(col).append(" TEXT, ");
        }
        ddl.append(
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        jdbcTemplate.execute(ddl.toString());
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_"
                        + tableName + "_file_id ON "
                        + tableName + " (file_id)");

        DatasetSchema schema = new DatasetSchema();
        schema.setSchemaHash(hash);
        schema.setTableName(tableName);
        schema.setDataCategory(category);
        schema.setColumnDefinitions(
                String.join(",", columns));
        schema.setCreatedAt(LocalDateTime.now());
        return datasetSchemaRepository.save(schema);
    }

    private String computeSchemaHash(
            String category, List<String> columns) {
        List<String> normalized = columns.stream()
                .map(c -> sanitizeIdentifier(c, "col"))
                .collect(Collectors.toList());
        String base = category.trim().toLowerCase()
                + "|" + String.join(",", normalized);
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");
            byte[] hash = digest.digest(
                    base.getBytes(
                            StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format(
                        "%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to hash schema", e);
        }
    }

    private List<String> buildSafeColumnNames(
            List<String> columns) {
        List<String> safe = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String col : columns) {
            String base = sanitizeIdentifier(
                    col, "col");
            String candidate = base;
            int suffix = 2;
            while (used.contains(candidate)) {
                candidate = base + "_" + suffix;
                suffix++;
            }
            used.add(candidate);
            safe.add(candidate);
        }
        return safe;
    }

    private List<String> getSafeColumnNames(
            String originalColumnsCsv) {
        List<String> columns = Arrays.asList(
                originalColumnsCsv.split(","));
        return buildSafeColumnNames(columns);
    }

    private String sanitizeIdentifier(
            String raw, String fallbackPrefix) {
        String s = raw.trim().toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");
        if (s.isEmpty() ||
                Character.isDigit(s.charAt(0))) {
            s = fallbackPrefix + "_" + s;
        }
        if (s.length() > 55) {
            s = s.substring(0, 55);
        }
        return s;
    }

    // ==========================================
    // INSERT ROWS INTO DYNAMIC TABLE (BATCHED)
    // ==========================================
    private void insertRowsIntoTable(
            String tableName,
            List<String> safeColumns,
            List<String> originalColumns,
            Long fileId,
            List<Map<String, Object>> rows) {

        if (rows.isEmpty()) return;

        String placeholders = safeColumns.stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName
                + " (file_id, row_number, "
                + String.join(", ", safeColumns)
                + ") VALUES (?, ?, "
                + placeholders + ")";

        List<Object[]> batchArgs = new ArrayList<>();
        int rowNum = 1;
        for (Map<String, Object> row : rows) {
            Object[] args = new Object[
                    2 + originalColumns.size()];
            args[0] = fileId;
            args[1] = rowNum++;
            for (int i = 0;
                 i < originalColumns.size(); i++) {
                Object value = row.get(
                        originalColumns.get(i));
                args[2 + i] = value == null
                        ? null : String.valueOf(value);
            }
            batchArgs.add(args);
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    // ==========================================
    // GET ALL DATA FILES — Excel/CSV (dynamic)
    // + PDF (legacy) COMBINED, scoped to user
    // ==========================================
    public List<Map<String, Object>> getAllDataFiles(
            String userEmail, boolean isAdmin) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow();

        List<Map<String, Object>> result =
                new ArrayList<>();

        // Excel/CSV files from the new system
        List<DatasetFile> datasetFiles =
                datasetFileRepository
                        .findByUploadedById(
                                user.getId());
        for (DatasetFile f : datasetFiles) {
            result.add(toFileInfoMap(f));
        }

        // PDF files from the legacy system
        List<String> pdfFileNames =
                dataRecordRepository
                        .findDistinctFilesByUser(
                                user.getId());
        for (String fileName : pdfFileNames) {
            List<DataRecord> records =
                    dataRecordRepository
                            .findBySourceFileNameAndUploadedById(
                                    fileName,
                                    user.getId());
            if (!records.isEmpty() &&
                    "PDF".equals(records.get(0)
                            .getSourceFileType())) {
                result.add(toLegacyFileInfoMap(
                        fileName, records));
            }
        }

        return result;
    }

    // ==========================================
    // GET FILES FILTERED BY DATE
    // ==========================================
    public List<Map<String, Object>> getFilesByFilter(
            String userEmail,
            boolean isAdmin,
            Integer year,
            Integer month,
            Integer day,
            String fileType) {

        if (year == null || month == null) {
            return new ArrayList<>();
        }

        // Simplest reliable approach: reuse the
        // full user file list, then filter in
        // memory by date/type — avoids duplicating
        // date-matching logic across two storage
        // systems (dynamic tables + legacy)
        List<Map<String, Object>> all =
                getAllDataFiles(userEmail, isAdmin);

        return all.stream()
                .filter(f -> {
                    Object y = f.get("uploadYear");
                    Object m = f.get("uploadMonth");
                    Object d = f.get("uploadDay");
                    if (!(y instanceof Integer) ||
                            !year.equals(y))
                        return false;
                    if (!(m instanceof Integer) ||
                            !month.equals(m))
                        return false;
                    if (day != null &&
                            (!(d instanceof Integer)
                                    || !day.equals(d)))
                        return false;
                    if (fileType != null &&
                            !fileType.isEmpty() &&
                            !fileType.equals("ALL") &&
                            !fileType.equalsIgnoreCase(
                                    (String) f.get(
                                            "fileType")))
                        return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> toFileInfoMap(
            DatasetFile f) {
        Map<String, Object> info =
                new LinkedHashMap<>();
        info.put("fileName", f.getFileName());
        info.put("fileType", f.getFileType());
        info.put("dataCategory",
                f.getDataCategory());
        info.put("totalRecords", f.getTotalRows());
        info.put("uploadedBy",
                f.getUploadedBy().getFullName());
        info.put("uploadedAt",
                f.getCreatedAt().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm")));
        info.put("uploadYear", f.getUploadYear());
        info.put("uploadMonth", f.getUploadMonth());
        info.put("uploadDay", f.getUploadDay());
        info.put("eodDate", f.getEodDate());
        info.put("status", "imported");
        return info;
    }

    private Map<String, Object> toLegacyFileInfoMap(
            String fileName, List<DataRecord> records) {
        DataRecord first = records.get(0);
        // Count distinct rows, not raw key-value cells
        long distinctRows = records.stream()
                .map(DataRecord::getRowNumber)
                .distinct()
                .count();
        Map<String, Object> info =
                new LinkedHashMap<>();
        info.put("fileName", fileName);
        info.put("fileType",
                first.getSourceFileType());
        info.put("dataCategory",
                first.getDataCategory());
        info.put("totalRecords", (int) distinctRows);
        info.put("uploadedBy",
                first.getUploadedBy().getFullName());
        info.put("uploadedAt",
                first.getCreatedAt().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm")));
        info.put("uploadYear", null);
        info.put("uploadMonth", null);
        info.put("uploadDay", null);
        info.put("eodDate", null);
        info.put("status", "imported");
        return info;
    }

    // ==========================================
    // VIEW EXTRACTED DATA — scoped to user
    // (checks dynamic tables first, then legacy)
    // ==========================================
    public DataRecordResponse getExtractedData(
            String fileName,
            String userEmail,
            boolean isAdmin) {

        String normalized = normalizeFileName(
                fileName);

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow();

        Optional<DatasetFile> datasetFile =
                datasetFileRepository
                        .findByFileNameAndUploadedById(
                                fileName, user.getId())
                        .or(() -> datasetFileRepository
                                .findByFileNameAndUploadedById(
                                        normalized,
                                        user.getId()));

        if (datasetFile.isPresent()) {
            return loadFileData(datasetFile.get());
        }

        // Fall back to legacy PDF storage
        return getExtractedDataLegacy(
                fileName, userEmail);
    }

    public DataRecordResponse getExtractedData(
            String fileName) {
        // Legacy overload — kept for compatibility,
        // no longer called by the controller
        List<DataRecord> records =
                dataRecordRepository
                        .findBySourceFileName(fileName);
        return buildLegacyResponse(fileName, records);
    }

    // ====== NEW MISSING METHOD ADDED HERE ======
    private DataRecordResponse loadFileData(
            DatasetFile datasetFile) {

        DatasetSchema schema =
                datasetFile.getSchema();
        List<String> originalColumns =
                Arrays.asList(schema
                        .getColumnDefinitions()
                        .split(","));
        List<String> safeColumns =
                getSafeColumnNames(
                        schema.getColumnDefinitions());

        String selectCols =
                String.join(", ", safeColumns);
        String sql = "SELECT row_number, "
                + selectCols + " FROM "
                + schema.getTableName()
                + " WHERE file_id = ? "
                + "ORDER BY row_number";

        List<Map<String, Object>> rows =
                jdbcTemplate.query(sql,
                        (rs, rowNum) -> {
                            Map<String, Object> row =
                                    new LinkedHashMap<>();
                            for (int i = 0;
                                 i < originalColumns
                                         .size(); i++) {
                                String rawValue = rs.getString(
                                        safeColumns.get(i));
                                row.put(
                                        originalColumns.get(i),
                                        detectAndConvert(
                                                rawValue));
                            }
                            return row;
                        }, datasetFile.getId());

        DataRecordResponse response =
                new DataRecordResponse();
        response.setSourceFileName(
                datasetFile.getFileName());
        response.setSourceFileType(
                datasetFile.getFileType());
        response.setDataCategory(
                datasetFile.getDataCategory());
        response.setTotalRows(rows.size());
        response.setTotalColumns(
                originalColumns.size());
        response.setColumns(originalColumns);
        response.setRows(rows);
        response.setUploadedBy(datasetFile
                .getUploadedBy().getFullName());
        response.setUploadedAt(datasetFile
                .getCreatedAt().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm")));
        response.setMessage("Data loaded!");
        return response;
    }
    // ==========================================

    private DataRecordResponse getExtractedDataLegacy(
            String fileName, String userEmail) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow();

        List<DataRecord> records =
                dataRecordRepository
                        .findBySourceFileNameAndUploadedById(
                                fileName, user.getId());

        if (records.isEmpty()) {
            String normalized =
                    normalizeFileName(fileName);
            records = dataRecordRepository
                    .findBySourceFileNameAndUploadedById(
                            normalized, user.getId());
        }

        if (records.isEmpty()) {
            DataRecordResponse denied =
                    new DataRecordResponse();
            denied.setMessage(
                    "Access denied or file not found: "
                            + fileName);
            return denied;
        }

        return buildLegacyResponse(fileName, records);
    }

    private DataRecordResponse buildLegacyResponse(
            String fileName, List<DataRecord> records) {

        if (records.isEmpty()) {
            DataRecordResponse response =
                    new DataRecordResponse();
            response.setMessage(
                    "No data found for: " + fileName);
            return response;
        }

        Set<String> colSet = new LinkedHashSet<>();
        Map<Integer, Map<String, Object>> rowMap =
                new TreeMap<>();

        for (DataRecord record : records) {
            colSet.add(record.getColumnName());
            rowMap.computeIfAbsent(
                    record.getRowNumber(),
                    k -> new LinkedHashMap<>()
            ).put(record.getColumnName(),
                    detectAndConvert(
                            record.getValue()));
        }

        List<String> columns =
                new ArrayList<>(colSet);
        List<Map<String, Object>> rows =
                new ArrayList<>(rowMap.values());

        DataRecord first = records.get(0);
        DataRecordResponse response =
                new DataRecordResponse();
        response.setSourceFileName(fileName);
        response.setSourceFileType(
                first.getSourceFileType());
        response.setDataCategory(
                first.getDataCategory());
        response.setTotalRows(rows.size());
        response.setTotalColumns(columns.size());
        response.setColumns(columns);
        response.setRows(rows);
        response.setUploadedBy(
                first.getUploadedBy().getFullName());
        response.setUploadedAt(
                first.getCreatedAt().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm")));
        response.setMessage("Data loaded!");
        return response;
    }

    // ==========================================
    // DELETE FILE — checks dynamic tables first,
    // then legacy PDF storage
    // ==========================================
    @Transactional
    public String deleteFileData(
            String fileName,
            String userEmail,
            boolean isAdmin) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow();

        String normalized =
                normalizeFileName(fileName);

        Optional<DatasetFile> datasetFile =
                datasetFileRepository
                        .findByFileNameAndUploadedById(
                                fileName, user.getId())
                        .or(() -> datasetFileRepository
                                .findByFileNameAndUploadedById(
                                        normalized,
                                        user.getId()));

        if (datasetFile.isPresent()) {
            removeDatasetFile(datasetFile.get());
            return "Deleted successfully!";
        }

        // Fall back to legacy PDF deletion
        boolean owns = !dataRecordRepository
                .findBySourceFileNameAndUploadedById(
                        fileName, user.getId())
                .isEmpty();
        if (!owns) {
            owns = !dataRecordRepository
                    .findBySourceFileNameAndUploadedById(
                            normalized, user.getId())
                    .isEmpty();
            if (owns) fileName = normalized;
        }

        if (!owns) {
            throw new RuntimeException(
                    "File not found or you do not "
                            + "own it: " + fileName);
        }

        dataRecordRepository
                .deleteBySourceFileNameAndUploadedById(
                        fileName, user.getId());
        return "Deleted successfully!";
    }

    private void removeDatasetFile(
            DatasetFile datasetFile) {
        jdbcTemplate.update(
                "DELETE FROM "
                        + datasetFile.getTableName()
                        + " WHERE file_id = ?",
                datasetFile.getId());
        datasetFileRepository.delete(datasetFile);
    }

    @Transactional
    public void deleteAllDataForUser(Long userId) {
        List<DatasetFile> files =
                datasetFileRepository
                        .findByUploadedById(userId);
        for (DatasetFile f : files) {
            removeDatasetFile(f);
        }
        dataRecordRepository
                .deleteByUploadedById(userId);
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private String normalizeFileName(
            String rawFileName) {
        return rawFileName.replaceAll(
                "\\s*\\(\\d+\\)(?=\\.[a-zA-Z0-9]+$)",
                "");
    }

    private Object detectAndConvert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {}
        return value.trim();
    }

    private String getDataType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "STRING";
        }
        try {
            Integer.parseInt(value.trim());
            return "INTEGER";
        } catch (NumberFormatException ignored) {}
        try {
            Double.parseDouble(value.trim());
            return "FLOAT";
        } catch (NumberFormatException ignored) {}
        return "STRING";
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(
                        cell)) {
                    return cell
                            .getLocalDateTimeCellValue()
                            .toString();
                }
                double num =
                        cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return String.valueOf(
                            (long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(
                        cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(
                            cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}