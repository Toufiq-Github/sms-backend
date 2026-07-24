package com.spectrum.smsbackend.service;

import com.spectrum.smsbackend.dto.ChangePasswordRequest;
import com.spectrum.smsbackend.dto.ProfileResponse;
import com.spectrum.smsbackend.dto.UpdateProfileRequest;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Get profile by email
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getLastLogin(),
                user.getCreatedAt()
        );
    }

    // Update full name
    public String updateProfile(String email,
                                UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow();

        user.setFullName(request.getFullName());
        userRepository.save(user);

        return "Profile updated successfully!";
    }

    // Change password
    public String changePassword(String email,
                                 ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow();

        // Verify current password matches
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {
            return "Current password is incorrect!";
        }

        // Encrypt and save new password
        user.setPassword(passwordEncoder.encode(
                request.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully!";
    }
}