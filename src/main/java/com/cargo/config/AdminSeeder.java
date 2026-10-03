package com.cargo.config;

import com.cargo.entity.User;
import com.cargo.enums.Role;
import com.cargo.repository.UserRepository;
import com.cargo.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a single admin account on first startup so there's always a way in.
 * Login: admin@cargo.com / Admin@123
 * (Meets the same 8-char + special-character rule enforced everywhere else,
 * and is stored as a BCrypt hash like every other password.)
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminSeeder(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@cargo.com")) {
            return;
        }
        User admin = new User();
        admin.setFullName("System Admin");
        admin.setEmail("admin@cargo.com");
        admin.setPassword("Admin@123");
        admin.setPhone("9999999999");
        admin.setRole(Role.ADMIN);
        userService.registerPreValidatedUser(admin);
    }
}
