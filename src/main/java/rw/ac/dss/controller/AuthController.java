package rw.ac.dss.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.ac.dss.dto.request.LoginRequest;
import rw.ac.dss.dto.request.RegisterRequest;
import rw.ac.dss.dto.request.UpdateProfileRequest;
import rw.ac.dss.dto.request.UpdateUserRequest;
import rw.ac.dss.dto.response.AuthResponseDto;
import rw.ac.dss.dto.response.UserResponseDto;
import rw.ac.dss.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/users")
    public List<UserResponseDto> listUsers() {
        return authService.listUsers();
    }

    @PutMapping("/users/{id}")
    public UserResponseDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return authService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponseDto getOwnProfile(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponseDto updateOwnProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateOwnProfile(authentication.getName(), request);
    }
}
