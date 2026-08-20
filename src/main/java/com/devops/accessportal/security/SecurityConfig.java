package com.devops.accessportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {

        return (request, response, authentication) -> {

            boolean isEmployee = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority()
                                    .equals("ROLE_EMPLOYEE"));

            boolean isReviewer = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority()
                                    .equals("ROLE_REVIEWER"));

            if (isEmployee) {

                response.sendRedirect("/employee/dashboard");

            } else if (isReviewer) {

                response.sendRedirect("/reviewer/dashboard");

            } else {

                response.sendRedirect("/login?error");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                .requestMatchers("/employee/**")
                    .hasRole("EMPLOYEE")

                .requestMatchers("/reviewer/**")
                    .hasRole("REVIEWER")

                .anyRequest()
                    .authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}