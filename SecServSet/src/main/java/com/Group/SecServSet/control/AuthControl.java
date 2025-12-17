package com.Group.SecServSet.control;

import com.Group.SecServSet.model.Role;
import com.Group.SecServSet.model.User;
import com.Group.SecServSet.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthControl {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthControl(UserRepo userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // Register a new user endpoint.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // endpoint validation/error messages
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


        Role requestedRole = user.getRole() == null ? Role.ROLE_USER : user.getRole();

        // only if the database has no admin, can an admin be registered
        if (requestedRole == Role.ROLE_ADMIN) {
            if (userRepo.existsByRole(Role.ROLE_ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin registration is disabled. The first admin has already been created."));
            }
            final String ADMIN_REG_SECRET = "SUPER-SECRET-ADMIN-CODE";
            if (user.getAdminSecret() == null || user.getAdminSecret().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Admin secret is required for admin registration"));
            }
            if (!ADMIN_REG_SECRET.equals(user.getAdminSecret())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid admin secret"));
            }
        }




        user.setRole(requestedRole);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepo.save(user);

        return ResponseEntity.status(201)
                .body(Map.of("message", "User registered", "username", user.getUsername()));
    }

    //login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {

        String username = body.get("username");
        String password = body.get("password");

        // Check if user is already authenticated
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated()
                && !currentAuth.getPrincipal().equals("anonymousUser")) {

            String currentUsername = currentAuth.getName();

            // If trying to log in as the same user
            if (currentUsername.equals(username)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "Already logged in as " + currentUsername,
                                "message", "Please logout first or continue with current session"
                        ));
            }

            // If trying to log in as a different user
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Already logged in as " + currentUsername,
                            "message", "Please logout before logging in as a different user"
                    ));
        }

        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Create new security context
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Save to session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", authentication.getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

}