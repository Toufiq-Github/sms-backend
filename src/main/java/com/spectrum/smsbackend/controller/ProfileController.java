package com.spectrum.smsbackend.controller;

import com.spectrum.smsbackend.dto.ChangePasswordRequest;
import com.spectrum.smsbackend.dto.ProfileResponse;
import com.spectrum.smsbackend.dto.UpdateProfileRequest;
import com.spectrum.smsbackend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // Get current user profile
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            Authentication authentication) {
        String email = authentication.getName();
        ProfileResponse profile =
                profileService.getProfile(email);
        return ResponseEntity.ok(profile);
    }

    // Update full name
    @PutMapping("/update")
    public ResponseEntity<String> updateProfile(
            @Valid @RequestBody
            UpdateProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        String result = profileService
                .updateProfile(email, request);
        return ResponseEntity.ok(result);
    }

    // Change password
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody
            ChangePasswordRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        String result = profileService
                .changePassword(email, request);
        return ResponseEntity.ok(result);
    }
}