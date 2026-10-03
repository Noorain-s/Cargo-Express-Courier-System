package com.cargo.service;

import com.cargo.entity.User;
import com.cargo.enums.Role;
import com.cargo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Shared bean defined in SecurityConfig - same encoder used to verify
    // passwords at login time via Spring Security's DaoAuthenticationProvider.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Password rule: at least 8 characters and at least one special character
    private static final Pattern PASSWORD_RULE =
            Pattern.compile("^(?=.*[^a-zA-Z0-9]).{8,}$");

    // Phone rule: exactly 10 digits
    private static final Pattern PHONE_RULE = Pattern.compile("^\\d{10}$");

    // Registers a new user (Admin creates Staff, or Customer self-registers)
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + user.getEmail());
        }
        if (user.getPassword() == null || !PASSWORD_RULE.matcher(user.getPassword()).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include at least one special character (e.g. @, #, !, %).");
        }
        if (user.getPhone() == null || !PHONE_RULE.matcher(user.getPhone()).matches()) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        // Never store the raw password - only the BCrypt hash goes to the database
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Registers a user whose password is already known to satisfy the rules
    // and should be stored as-is except for hashing (used by the admin seeder).
    public User registerPreValidatedUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
