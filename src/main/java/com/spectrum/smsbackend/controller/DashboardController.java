package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.dto.DashboardStats;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // Get dashboard stats — scoped to logged-in user
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(
            Authentication authentication) {
        String role = authentication
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();
        String email = authentication.getName();
        DashboardStats stats =
                dashboardService.getStats(role, email);
        return ResponseEntity.ok(stats);
    }

    // Recent logins — Admin only
    @GetMapping("/recent-logins")
    public ResponseEntity<List<User>> getRecentLogins(
            Authentication authentication) {
        String role = authentication
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();
        if (!role.equals("ROLE_ADMIN")) {
            return ResponseEntity
                    .status(403).build();
        }
        List<User> recentLogins =
                dashboardService.getRecentLogins();
        return ResponseEntity.ok(recentLogins);
    }
}