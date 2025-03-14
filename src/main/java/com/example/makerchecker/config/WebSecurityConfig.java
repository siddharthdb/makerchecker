package com.example.makerchecker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()  // Disable CSRF for simplicity in the API
                .authorizeHttpRequests((authz) -> authz
                        // Allow access to H2 console if used
                        .requestMatchers("/h2-console/**").permitAll()
                        // Allow access to Camunda web apps
                        .requestMatchers("/camunda/**").permitAll()
                        // Protect API endpoints
                        .requestMatchers("/api/**").authenticated()
                        // Allow everything else
                        .anyRequest().permitAll()
                )
                .httpBasic();  // Enable Basic Authentication

        // Allow frames for H2 console
        http.headers().frameOptions().sameOrigin();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Create default users for testing
        UserDetails maker1 = User.builder()
                .username("maker1")
                .password(passwordEncoder().encode("password"))
                .roles("MAKER")
                .build();

        UserDetails maker2 = User.builder()
                .username("maker2")
                .password(passwordEncoder().encode("password"))
                .roles("MAKER")
                .build();

        UserDetails checker1 = User.builder()
                .username("checker1")
                .password(passwordEncoder().encode("password"))
                .roles("CHECKER")
                .build();

        UserDetails checker2 = User.builder()
                .username("checker2")
                .password(passwordEncoder().encode("password"))
                .roles("CHECKER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN", "MAKER", "CHECKER")
                .build();

        return new InMemoryUserDetailsManager(maker1, maker2, checker1, checker2, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}