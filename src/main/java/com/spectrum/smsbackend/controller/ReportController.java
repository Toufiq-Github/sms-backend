package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.model.Report;
import com.spectrum.smsbackend.service.ReportService;
import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // Generate PDF report
    @GetMapping("/generate/pdf")
    public ResponseEntity<Resource> generatePdfReport(
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String filePath = reportService.generatePdfReport(userEmail);
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IOException | DocumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Generate Excel report
    @GetMapping("/generate/excel")
    public ResponseEntity<Resource> generateExcelReport(
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String filePath = reportService.generateExcelReport(userEmail);
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument"
                                    + ".spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ REPLACED: Get all reports — only the logged-in user's own reports
    @GetMapping("/list")
    public ResponseEntity<List<Report>> getAllReports(
            Authentication authentication) {
        String userEmail = authentication.getName();
        List<Report> reports = reportService.getAllReports(userEmail);
        return ResponseEntity.ok(reports);
    }
}