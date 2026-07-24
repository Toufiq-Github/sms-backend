package com.spectrum.smsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStats {

    private Long totalUsers;
    private Long activeUsers;
    private Long totalFiles;
    private Long totalReports;
    private String message;
}