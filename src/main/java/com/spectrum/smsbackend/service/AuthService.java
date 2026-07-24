package com.spectrum.smsbackend.service;

import com.spectrum.smsbackend.dto.LoginRequest;
import com.spectrum.smsbackend.dto.LoginResponse;
import com.spectrum.smsbackend.dto.RegisterRequest;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.UserRepository;
import com.spectrum.smsbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    // Register new user
    public String register(RegisterRequest request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email already registered!";
        }

        // Create new user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        return "User registered successfully!";
    }

    // Login user
    public LoginResponse login(LoginRequest request) {

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        if (authentication.isAuthenticated()) {
            // Update last login time
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(request.getEmail());

            return new LoginResponse(
                    token,
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    "Login successful!"
            );
        }

        return new LoginResponse(
                null, null, null, null, "Invalid credentials!");
    }
}