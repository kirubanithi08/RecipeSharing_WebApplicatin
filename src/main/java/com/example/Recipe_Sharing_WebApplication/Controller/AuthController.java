package com.example.Recipe_Sharing_WebApplication.Controller;

import com.example.Recipe_Sharing_WebApplication.DTO.AuthRequest;
import com.example.Recipe_Sharing_WebApplication.DTO.AuthResponse;
import com.example.Recipe_Sharing_WebApplication.DTO.RefreshTokenRequest;
import com.example.Recipe_Sharing_WebApplication.DTO.RegisterRequest;
import com.example.Recipe_Sharing_WebApplication.Entity.Role;
import com.example.Recipe_Sharing_WebApplication.Entity.User;
import com.example.Recipe_Sharing_WebApplication.Repository.UserRepository;
import com.example.Recipe_Sharing_WebApplication.Service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final String ADMIN_USERNAME = "kiruba";
    private static final String ADMIN_PASSWORD = "123";


    @PostConstruct
    public void initAdmin() {
        if (userRepository.findByUsername(ADMIN_USERNAME).isEmpty()) {
            User admin = User.builder()
                    .username(ADMIN_USERNAME)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Always USER
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {


        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );


        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow();

        if (!jwtService.isTokenValid(request.getRefreshToken(), user)) {
            return ResponseEntity.status(401).build();
        }

        String newAccessToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(newAccessToken, request.getRefreshToken()));
    }
}

