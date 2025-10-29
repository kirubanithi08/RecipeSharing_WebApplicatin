package com.example.Recipe_Sharing_WebApplication.Controller;

import com.example.Recipe_Sharing_WebApplication.DTO.AuthRequest;
import com.example.Recipe_Sharing_WebApplication.DTO.AuthResponse;
import com.example.Recipe_Sharing_WebApplication.DTO.RefreshTokenRequest;
import com.example.Recipe_Sharing_WebApplication.DTO.RegisterRequest;
import com.example.Recipe_Sharing_WebApplication.Entity.Role;
import com.example.Recipe_Sharing_WebApplication.Entity.User;
import com.example.Recipe_Sharing_WebApplication.Repository.UserRepository;
import com.example.Recipe_Sharing_WebApplication.Service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.util.Map;

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

//    @PostMapping("/register")
//    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
//        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
//            return ResponseEntity.badRequest().body("Username already exists!");
//        }
//
//        User user = User.builder()
//                .username(request.getUsername())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(Role.USER)
//                .build();
//
//        userRepository.save(user);
//        return ResponseEntity.ok("User registered successfully!");
//
//
//    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists!"));
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request,
            HttpServletResponse response) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // ✅ Set secure HttpOnly cookie for refresh token
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)  // ✅ Change to true when using HTTPS
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(new AuthResponse(accessToken, null)); // No more JSON refresh token
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        // ✅ Read refresh token from cookie (not JSON body anymore)
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(null);
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow();

        if (!jwtService.isTokenValid(refreshToken, user)) {
            return ResponseEntity.status(401).body(null);
        }

        // ✅ Generate new tokens
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // ✅ Update cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(new AuthResponse(newAccessToken, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // ✅ Delete the cookie
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok("Logged out");
    }
}
