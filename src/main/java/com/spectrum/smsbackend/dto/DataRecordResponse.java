package com.spectrum.smsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRecordResponse {

    private String sourceFileName;
    private String sourceFileType;
    private String dataCategory;
    private int totalRows;
    private int totalColumns;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private String uploadedBy;
    private String uploadedAt;
    private String message;
}