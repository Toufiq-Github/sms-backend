package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.DataRecordRepository;
import com.spectrum.smsbackend.repository.FileRepository;
import com.spectrum.smsbackend.repository.ReportRepository;
import com.spectrum.smsbackend.repository.UserRepository;
import com.spectrum.smsbackend.service.DataExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DataExtractionService dataExtractionService;

    // Get all users - Admin only
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // Delete user - Admin only
    @Transactional
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Step 1 — clean up the new dynamic
            // dataset tables owned by this user
            dataExtractionService
                    .deleteAllDataForUser(id);

            // Step 2 — safety net: clean up any
            // legacy data_records rows from before
            // the dynamic-table architecture
            dataRecordRepository
                    .deleteByUploadedById(id);

            // Step 3 — delete reports generated
            // by this user
            reportRepository.findByGeneratedById(id)
                    .forEach(report ->
                            reportRepository
                                    .delete(report));

            // Step 4 — delete files uploaded by
            // this user
            fileRepository.findByUploadedById(id)
                    .forEach(file ->
                            fileRepository
                                    .delete(file));

            // Step 5 — now safe to delete the user
            userRepository.deleteById(id);

            return ResponseEntity.ok(
                    "User deleted successfully!");

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Delete failed: "
                            + e.getMessage());
        }
    }

    // Toggle user active status - Admin only
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<String> toggleUserStatus(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow();
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(
                "User status updated successfully!");
    }

    // Promote user to Admin - Admin only
    @PutMapping("/users/{id}/make-admin")
    public ResponseEntity<String> makeAdmin(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
        return ResponseEntity.ok(
                "User promoted to Admin successfully!");
    }
}