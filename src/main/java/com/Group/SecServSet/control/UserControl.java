package com.Group.SecServSet.control;

import com.Group.SecServSet.model.User;
import com.Group.SecServSet.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserControl {
    @Autowired
    private UserRepo userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // -------- CREATE USER (admin only) ----------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return ResponseEntity.status(201).body(Map.of("message", "User created", "username", user.getUsername()));
    }

    // -------- LIST USERS (ADMIN ONLY) ----------
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<User> all = userRepository.findAll();
        if (all.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Users table is empty"));
        }
        return ResponseEntity.ok(all);
    }

    // -------- GET USER (USER can view self, ADMIN can view anyone) ----------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> getUser(@PathVariable Long id) {

        Optional<User> opt = userRepository.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = opt.get();

        // Get authenticated user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Block normal users from viewing others
        if (!isAdmin && !authenticatedUsername.equals(user.getUsername())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You can only view your own profile"));
        }

        return ResponseEntity.ok(user);
    }

    // -------- UPDATE USER (USER updates self, ADMIN updates anyone) ----------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {

        Optional<User> opt = userRepository.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User existing = opt.get();

        // Authenticated user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // -------- BLOCK NORMAL USERS FROM UPDATING OTHERS ----------
        if (!isAdmin && !authenticatedUsername.equals(existing.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("error", "You may update ONLY your own profile"));
        }

        // -------- PREVENT PRIVILEGE ESCALATION ----------
        if (!isAdmin && updatedUser.getRole() != null && updatedUser.getRole() != existing.getRole()) {
            return ResponseEntity.status(403).body(Map.of("error", "You are not allowed to change your role"));
        }

        // -------- DUPLICATE USERNAME CHECK ----------
        if (updatedUser.getUsername() != null &&
                !existing.getUsername().equals(updatedUser.getUsername()) &&
                userRepository.existsByUsername(updatedUser.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        // -------- DUPLICATE EMAIL CHECK ----------
        if (updatedUser.getEmail() != null &&
                !existing.getEmail().equals(updatedUser.getEmail()) &&
                userRepository.existsByEmail(updatedUser.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // -------- APPLY CHANGES ----------
        if (updatedUser.getUsername() != null) {
            existing.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getEmail() != null) {
            existing.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getAge() != null) {
            existing.setAge(updatedUser.getAge());
        }

        // update password
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        // role may be updated only by ADMIN
        if (isAdmin && updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }

        userRepository.save(existing);

        return ResponseEntity.ok(Map.of("message", "User updated", "username", existing.getUsername()));
    }

    // -------- DELETE USER (ADMIN ONLY) ----------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {

        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted", "id", id));
    }
}