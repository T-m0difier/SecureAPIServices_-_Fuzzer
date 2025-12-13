package com.Group.SecServSet.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint customEntryPoint() {
        return new CustomAuthEntryPoint();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login", "/h2-console/**").permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/tasks/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        // ADDED — prevents session fixation
                        .sessionFixation().migrateSession()

                        // ADDED — 1 session per user
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/auth/login")   //  session login endpoint
                        .successHandler((req, res, auth) -> {
                            res.setStatus(200);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"message\": \"Login successful\"}");
                        })
                        .failureHandler((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"invalid credentials\"}");

                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")          //  session logout
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)        //  session invalidation
                        .deleteCookies("JSESSIONID")        //  remove cookie
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                );


        return http.build();
    }


}
