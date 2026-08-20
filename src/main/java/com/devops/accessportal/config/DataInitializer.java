package com.devops.accessportal.config;

import com.devops.accessportal.entity.User;
import com.devops.accessportal.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Create Employee user if it doesn't already exist
            if (!userRepository.existsByEmail("employee@accessportal.com")) {

                User employee = new User(
                        "Demo Employee",
                        "employee@accessportal.com",
                        passwordEncoder.encode("employee123"),
                        "IT",
                        "EMPLOYEE"
                );

                userRepository.save(employee);

                System.out.println(
                        "Demo Employee user created successfully."
                );
            }

            // Create Reviewer user if it doesn't already exist
            if (!userRepository.existsByEmail("reviewer@accessportal.com")) {

                User reviewer = new User(
                        "Demo Reviewer",
                        "reviewer@accessportal.com",
                        passwordEncoder.encode("reviewer123"),
                        "IT",
                        "REVIEWER"
                );

                userRepository.save(reviewer);

                System.out.println(
                        "Demo Reviewer user created successfully."
                );
            }
        };
    }
}