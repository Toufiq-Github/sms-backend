package com.spectrum.smsbackend.service;

import com.spectrum.smsbackend.dto.DashboardStats;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.FileRepository;
import com.spectrum.smsbackend.repository.ReportRepository;
import com.spectrum.smsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ReportRepository reportRepository;

    // Get stats based on role AND scoped to the logged-in user
    public DashboardStats getStats(String role, String userEmail) {

        if (role.equals("ROLE_ADMIN")) {
            // Admin sees system-wide totals
            Long totalUsers = userRepository.count();
            Long activeUsers = (long) userRepository
                    .findByIsActiveTrue().size();
            Long totalFiles = fileRepository.count();
            Long totalReports = reportRepository.count();
            return new DashboardStats(
                    totalUsers, activeUsers,
                    totalFiles, totalReports,
                    "Admin Dashboard");
        } else {
            // Regular user sees ONLY their own file/report counts
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow();
            Long totalFiles = fileRepository
                    .countByUploadedById(user.getId());
            Long totalReports = (long) reportRepository
                    .findByGeneratedById(user.getId()).size();
            return new DashboardStats(
                    0L, 0L,
                    totalFiles, totalReports,
                    "User Dashboard");
        }
    }

    // Get recent logins - Admin only
    public List<User> getRecentLogins() {
        return userRepository.findRecentLogins();
    }
}