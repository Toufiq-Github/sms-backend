package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.model.UploadedFile;
import com.spectrum.smsbackend.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:4200")
public class FileController {

    @Autowired
    private FileService fileService;

    // Upload file
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String result = fileService.uploadFile(file, userEmail);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body("File upload failed: " + e.getMessage());
        }
    }

    // Get files — only the logged-in user's own files
    @GetMapping("/list")
    public ResponseEntity<List<UploadedFile>> getAllFiles(
            Authentication authentication) {
        String userEmail = authentication.getName();
        List<UploadedFile> files = fileService.getAllFiles(userEmail);
        return ResponseEntity.ok(files);
    }

    // Download file
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            UploadedFile uploadedFile = fileService.getFileById(id);
            Path filePath = Paths.get(uploadedFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\""
                                        + uploadedFile.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete file
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        try {
            String result = fileService.deleteFile(id);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body("File deletion failed: " + e.getMessage());
        }
    }
}