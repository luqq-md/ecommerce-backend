package com.luqman.ecommerce.auth.service;

import com.luqman.ecommerce.auth.dto.*;
import com.luqman.ecommerce.auth.entity.Role;
import com.luqman.ecommerce.auth.entity.User;
import com.luqman.ecommerce.auth.repository.UserRepository;
import com.luqman.ecommerce.auth.security.JwtService;
import com.luqman.ecommerce.auth.security.UserPrincipal;
import com.luqman.ecommerce.common.exception.BadRequestException;
import com.luqman.ecommerce.common.exception.ConflictException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                Role.USER
        );
        User saved = userRepository.save(user);
        return buildAuthResponse(new UserPrincipal(saved));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return buildAuthResponse(principal);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtService.isValidToken(request.refreshToken(), "refresh")) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String email = jwtService.extractEmail(request.refreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User no longer exists"));
        return buildAuthResponse(new UserPrincipal(user));
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                new UserResponse(
                        principal.getId(),
                        principal.getEmail(),
                        principal.getFullName(),
                        principal.getRole()
                )
        );
    }
}