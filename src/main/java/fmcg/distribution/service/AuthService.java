package fmcg.distribution.service;

import fmcg.distribution.dto.*;
import fmcg.distribution.entity.User;
import fmcg.distribution.entity.PasswordReset;
import fmcg.distribution.repository.UserRepository;
import fmcg.distribution.repository.PasswordResetRepository;
import fmcg.distribution.security.JwtUtil;
import fmcg.distribution.util.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordValidator passwordValidator;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (!passwordValidator.isValid(request.getPassword())) {
            throw new RuntimeException(passwordValidator.getValidationMessage());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setCreditPeriodDays(request.getCreditPeriodDays());
        user.setIsActive(true);

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(token, mapToUserResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is inactive");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(token, mapToUserResponse(user));
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        
        if (user == null) {
            return "If the email exists, a reset code has been generated";
        }

        // Generate 6-digit code
        String resetCode = String.format("%06d", new Random().nextInt(1000000));

        // Delete old reset codes
        passwordResetRepository.deleteByEmail(request.getEmail());

        // Create new reset record
        PasswordReset reset = new PasswordReset();
        reset.setEmail(request.getEmail());
        reset.setResetCode(resetCode);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetRepository.save(reset);

        return resetCode; // In production, send via email
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        PasswordReset reset = passwordResetRepository
                .findByEmailAndResetCode(request.getEmail(), request.getResetCode())
                .orElseThrow(() -> new RuntimeException("Invalid reset code"));

        if (LocalDateTime.now().isAfter(reset.getExpiresAt())) {
            throw new RuntimeException("Reset code has expired");
        }

        if (!passwordValidator.isValid(request.getNewPassword())) {
            throw new RuntimeException(passwordValidator.getValidationMessage());
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetRepository.deleteByEmail(request.getEmail());

        return "Password reset successfully";
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getCreditPeriodDays(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}