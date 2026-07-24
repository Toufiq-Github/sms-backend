package com.spectrum.smsbackend.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.spectrum.smsbackend.model.Report;
import com.spectrum.smsbackend.model.UploadedFile;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.FileRepository;
import com.spectrum.smsbackend.repository.ReportRepository;
import com.spectrum.smsbackend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    // Generate PDF Report
    public String generatePdfReport(String userEmail)
            throws IOException, DocumentException {

        Path reportPath = Paths.get(uploadDirectory + "/reports");
        if (!Files.exists(reportPath)) {
            Files.createDirectories(reportPath);
        }

        String fileName = "report_" + System.currentTimeMillis() + ".pdf";
        String filePath = reportPath + "/" + fileName;

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Title
        com.itextpdf.text.Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph title = new Paragraph(
                "Student Management System - File Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        // Date
        com.itextpdf.text.Font dateFont = FontFactory.getFont(
                FontFactory.HELVETICA, 12, BaseColor.GRAY);
        Paragraph date = new Paragraph("Generated on: " +
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                dateFont);
        document.add(date);
        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addTableHeader(table, "File Name");
        addTableHeader(table, "File Type");
        addTableHeader(table, "Size (KB)");
        addTableHeader(table, "Uploaded At");

        List<UploadedFile> files = fileRepository.findAll();
        for (UploadedFile file : files) {
            table.addCell(file.getFileName());
            table.addCell(file.getFileType());
            table.addCell(String.valueOf(file.getFileSize() / 1024));
            table.addCell(file.getUploadedAt().format(
                    DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        document.add(table);
        document.close();

        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Report report = new Report();
        report.setReportName(fileName);
        report.setReportType("PDF");
        report.setGeneratedBy(user);
        report.setFilePath(filePath);
        report.setCreatedAt(LocalDateTime.now());
        reportRepository.save(report);

        return filePath;
    }

    // Generate Excel Report
    public String generateExcelReport(String userEmail) throws IOException {

        Path reportPath = Paths.get(uploadDirectory + "/reports");
        if (!Files.exists(reportPath)) {
            Files.createDirectories(reportPath);
        }

        String fileName = "report_" + System.currentTimeMillis() + ".xlsx";
        String filePath = reportPath + "/" + fileName;

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("File Report");

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont =
                workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"File Name", "File Type", "Size (KB)", "Uploaded At"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<UploadedFile> files = fileRepository.findAll();
        int rowNum = 1;
        for (UploadedFile file : files) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(file.getFileName());
            row.createCell(1).setCellValue(file.getFileType());
            row.createCell(2).setCellValue(file.getFileSize() / 1024);
            row.createCell(3).setCellValue(
                    file.getUploadedAt().format(
                            DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
        workbook.close();

        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Report report = new Report();
        report.setReportName(fileName);
        report.setReportType("EXCEL");
        report.setGeneratedBy(user);
        report.setFilePath(filePath);
        report.setCreatedAt(LocalDateTime.now());
        reportRepository.save(report);

        return filePath;
    }

    // ✅ UPDATED: Get reports — SCOPED to the logged-in user only
    public List<Report> getAllReports(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return reportRepository.findByGeneratedById(user.getId());
    }

    // Helper for PDF table header
    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle));
        table.addCell(header);
    }
}