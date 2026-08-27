package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.ForgotPasswordRequest;
import rw.ac.dss.dto.request.LoginRequest;
import rw.ac.dss.dto.request.RegisterRequest;
import rw.ac.dss.dto.request.ResetPasswordRequest;
import rw.ac.dss.dto.request.UpdateProfileRequest;
import rw.ac.dss.dto.request.UpdateUserRequest;
import rw.ac.dss.dto.response.AuthResponseDto;
import rw.ac.dss.dto.response.ForgotPasswordResponseDto;
import rw.ac.dss.dto.response.UserResponseDto;
import rw.ac.dss.exception.ConflictException;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.User;
import rw.ac.dss.repository.UserRepository;
import rw.ac.dss.security.CustomUserDetailsService;
import rw.ac.dss.security.JwtService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDto register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword(request.getPassword());
        return login(loginRequest);
    }

    public AuthResponseDto login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String token = jwtService.generateToken(userDetails);

        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    /**
     * No email service is configured, so the raw token is returned directly to the
     * caller instead of being emailed - see ForgotPasswordResponseDto.
     */
    @Transactional
    public ForgotPasswordResponseDto forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("No account found for username: " + request.getUsername()));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES);
        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        return ForgotPasswordResponseDto.builder()
                .resetToken(token)
                .expiresAt(expiry)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Reset link is invalid or has already been used."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public List<UserResponseDto> listUsers() {
        return userRepository.findAll().stream().map(UserResponseDto::from).toList();
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (user.getRole() == User.Role.ADMIN && request.getRole() != User.Role.ADMIN && isLastEnabledAdmin(user)) {
            throw new ConflictException("Cannot change the role of the last remaining admin account.");
        }

        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) {
                throw new ConflictException("Password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return UserResponseDto.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (user.getRole() == User.Role.ADMIN && isLastEnabledAdmin(user)) {
            throw new ConflictException("Cannot delete the last remaining admin account.");
        }
        userRepository.delete(user);
    }

    public UserResponseDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return UserResponseDto.from(user);
    }

    /**
     * Self-service profile update - unlike updateUser(), this is available to every
     * role (not just ADMIN), can't change your own role/enabled status, and requires
     * your current password to set a new one.
     */
    @Transactional
    public UserResponseDto updateOwnProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ConflictException("Current password is incorrect.");
            }
            if (request.getNewPassword().length() < 8) {
                throw new ConflictException("New password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return UserResponseDto.from(userRepository.save(user));
    }

    private boolean isLastEnabledAdmin(User user) {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .allMatch(u -> u.getId().equals(user.getId()));
    }
}
