package com.chequemate.controller;

import com.chequemate.dto.LoginRequest;
import com.chequemate.dto.RegisterRequest;
import com.chequemate.entity.User;
import com.chequemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }

            String trimmedEmail = request.getEmail().trim().toLowerCase();

            Optional<User> existingUser = userRepository.findByEmail(trimmedEmail);
            if (existingUser.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered"));
            }

            User user = new User();
            user.setName(request.getName());
            user.setEmail(trimmedEmail);
            user.setPassword(request.getPassword());

            // Save first to obtain generated ID
            User savedUser = userRepository.saveAndFlush(user);

            // Format user code as A00001
            String userCode = "A" + String.format("%05d", savedUser.getId());
            savedUser.setUserCode(userCode);
            userRepository.saveAndFlush(savedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("userId", savedUser.getId());
            response.put("name", savedUser.getName());
            response.put("email", savedUser.getEmail());
            response.put("userCode", savedUser.getUserCode());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Registration error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email and password required"));
            }

            String trimmedEmail = request.getEmail().trim().toLowerCase();

            Optional<User> userOpt = userRepository.findByEmail(trimmedEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
            }

            User user = userOpt.get();

            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("userId", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("userCode", user.getUserCode() != null ? user.getUserCode() : "A" + String.format("%05d", user.getId()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Login error: " + e.getMessage()));
        }
    }
}