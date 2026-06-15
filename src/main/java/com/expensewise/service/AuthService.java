package com.expensewise.service;

import com.expensewise.config.DataInitializer;
import com.expensewise.config.JwtUtil;
import com.expensewise.dto.AuthRequest;
import com.expensewise.dto.AuthResponse;
import com.expensewise.dto.RegisterRequest;
import com.expensewise.entity.User;
import com.expensewise.exception.BadRequestException;
import com.expensewise.exception.ResourceNotFoundException;
import com.expensewise.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DataInitializer dataInitializer;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       DataInitializer dataInitializer,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dataInitializer = dataInitializer;
        this.emailService = emailService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .currency("INR")
                .build();

        user = userRepository.save(user);

        // Create default categories for the new user
        dataInitializer.createDefaultCategories(user.getId());

        String token = jwtUtil.generateToken(user.getEmail());

        // Send welcome email
        emailService.sendWelcomeEmail(user.getFullName(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
