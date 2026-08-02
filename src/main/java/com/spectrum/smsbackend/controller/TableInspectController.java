package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.repository.DatasetSchemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/inspect")
@CrossOrigin(origins = "http://localhost:4200")
public class TableInspectController {

    @Autowired
    private DatasetSchemaRepository datasetSchemaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Checks if the table name is a real, known
    // dynamic table before doing anything else with it
    private boolean isKnownTable(String tableName) {
        return datasetSchemaRepository.findAll()
                .stream()
                .anyMatch(s -> s.getTableName()
                        .equalsIgnoreCase(tableName));
    }

    // Step 1 — check existence + row count only,
    // no actual data returned yet
    @GetMapping("/lookup")
    public ResponseEntity<Map<String, Object>> lookupTable(
            @RequestParam String tableName) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (tableName == null ||
                tableName.trim().isEmpty() ||
                !isKnownTable(tableName.trim())) {
            result.put("found", false);
            result.put("tableName", tableName);
            result.put("rowCount", 0);
            result.put("message",
                    "No table found with that name.");
            return ResponseEntity.ok(result);
        }

        String safeName = tableName.trim();

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + safeName,
                Integer.class);

        result.put("found", true);
        result.put("tableName", safeName);
        result.put("rowCount",
                rowCount != null ? rowCount : 0);
        result.put("message",
                "Table found. " + rowCount
                        + " row(s) available.");
        return ResponseEntity.ok(result);
    }

    // Step 2 — actually load the raw table contents,
    // only called after lookup confirms it exists
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getTableData(
            @RequestParam String tableName) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (tableName == null ||
                tableName.trim().isEmpty() ||
                !isKnownTable(tableName.trim())) {
            result.put("found", false);
            result.put("columns", new ArrayList<>());
            result.put("rows", new ArrayList<>());
            result.put("message",
                    "No table found with that name.");
            return ResponseEntity.ok(result);
        }

        String safeName = tableName.trim();

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT * FROM " + safeName
                                + " ORDER BY id");

        List<String> columns = rows.isEmpty()
                ? getColumnNamesForEmptyTable(safeName)
                : new ArrayList<>(rows.get(0).keySet());

        result.put("found", true);
        result.put("tableName", safeName);
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("totalRows", rows.size());
        result.put("message",
                "Loaded " + rows.size() + " row(s).");
        return ResponseEntity.ok(result);
    }

    // Handles the edge case of a valid but empty table
    // — still show its columns even with zero rows
    private List<String> getColumnNamesForEmptyTable(
            String tableName) {
        return jdbcTemplate.query(
                "SELECT column_name FROM " +
                        "information_schema.columns " +
                        "WHERE table_name = ? " +
                        "ORDER BY ordinal_position",
                (rs, rowNum) -> rs.getString(
                        "column_name"),
                tableName);
    }
}