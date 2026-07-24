package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.dto.DataRecordResponse;
import com.spectrum.smsbackend.service.DataExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "http://localhost:4200")
public class DataUploadController {

    @Autowired
    private DataExtractionService dataExtractionService;

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_ADMIN"));
    }

    // Upload single file
    @PostMapping("/upload")
    public ResponseEntity<DataRecordResponse>
    uploadData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "eodDate",
                    defaultValue = "")
            String eodDate,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            DataRecordResponse response =
                    dataExtractionService
                            .extractAndSave(
                                    file, category,
                                    email, eodDate);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            DataRecordResponse error =
                    new DataRecordResponse();
            error.setMessage("Upload failed: "
                    + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(error);
        }
    }

    // Upload multiple files
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<DataRecordResponse>>
    uploadMultiple(
            @RequestParam("files")
            List<MultipartFile> files,
            @RequestParam("category") String category,
            @RequestParam(value = "eodDate",
                    defaultValue = "")
            String eodDate,
            Authentication authentication) {

        String email = authentication.getName();
        List<DataRecordResponse> responses =
                new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                DataRecordResponse response =
                        dataExtractionService
                                .extractAndSave(
                                        file, category,
                                        email, eodDate);
                responses.add(response);
            } catch (IOException e) {
                DataRecordResponse error =
                        new DataRecordResponse();
                error.setSourceFileName(
                        file.getOriginalFilename());
                error.setMessage(
                        "Failed: " + e.getMessage());
                responses.add(error);
            }
        }
        return ResponseEntity.ok(responses);
    }

    // Get own files only
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>>
    getAllDataFiles(
            Authentication authentication) {
        String email = authentication.getName();
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(
                dataExtractionService
                        .getAllDataFiles(
                                email, admin));
    }

    // Get own files filtered by date
    @GetMapping("/files/filter")
    public ResponseEntity<List<Map<String, Object>>>
    getFilteredFiles(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false)
            Integer day,
            @RequestParam(required = false,
                    defaultValue = "ALL")
            String fileType,
            Authentication authentication) {
        String email = authentication.getName();
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(
                dataExtractionService
                        .getFilesByFilter(
                                email, admin,
                                year, month,
                                day, fileType));
    }

    // View own file data only
    @GetMapping("/view")
    public ResponseEntity<DataRecordResponse>
    viewData(
            @RequestParam("fileName") String fileName,
            Authentication authentication) {
        String email = authentication.getName();
        boolean admin = isAdmin(authentication);
        DataRecordResponse response =
                dataExtractionService
                        .getExtractedData(
                                fileName,
                                email, admin);
        return ResponseEntity.ok(response);
    }

    // Delete own file only
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFileData(
            @RequestParam("fileName") String fileName,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            boolean admin = isAdmin(authentication);
            String result =
                    dataExtractionService
                            .deleteFileData(
                                    fileName,
                                    email, admin);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Delete failed: "
                            + e.getMessage());
        }
    }
}