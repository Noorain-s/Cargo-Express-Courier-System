package com.cargo.repository;

import com.cargo.entity.LoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findAllByOrderByLoginTimeDesc();
}
