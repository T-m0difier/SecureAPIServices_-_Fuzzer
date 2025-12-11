package com.Group.SecServSet.control;

import com.Group.SecServSet.model.Role;
import com.Group.SecServSet.model.User;
import com.Group.SecServSet.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthControl {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthControl(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new user. Default role is ROLE_USER unless explicitly specified.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // VALIDATION
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        if (userRepo.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        if (userRepo.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        if (user.getRole() == Role.ROLE_ADMIN) {
            String adminSecret = user.getAdminSecret();
            final String ADMIN_REG_SECRET = "SUPER-SECRET-ADMIN-CODE";

            if (adminSecret == null || !adminSecret.equals(ADMIN_REG_SECRET)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid admin secret. Cannot register as admin.")
                );
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) user.setRole(Role.ROLE_USER);

        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered", "username", user.getUsername()));
    }

}
