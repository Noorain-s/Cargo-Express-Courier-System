package com.cargo.repository;

import com.cargo.entity.User;
import com.cargo.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used for login: find by email
    Optional<User> findByEmail(String email);

    // Used for login validation: check email + password match
    Optional<User> findByEmailAndPassword(String email, String password);

    // Get all users of a specific role (e.g., list all Staff)
    List<User> findByRole(Role role);

    boolean existsByEmail(String email);
}
