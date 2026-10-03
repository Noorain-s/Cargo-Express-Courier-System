package com.cargo.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory brute-force protection. After 5 failed logins for an
 * email, that email is locked out for 15 minutes. Resets on success.
 * (In-memory is fine for a single-instance student/small deployment;
 * a real multi-server production system would use Redis instead.)
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockedUntil = new ConcurrentHashMap<>();

    public void loginFailed(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        int count = attempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(key, System.currentTimeMillis() + LOCK_DURATION_MS);
        }
    }

    public void loginSucceeded(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        attempts.remove(key);
        lockedUntil.remove(key);
    }

    public boolean isBlocked(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        Long until = lockedUntil.get(key);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            lockedUntil.remove(key);
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public int attemptsRemaining(String email) {
        if (email == null) return MAX_ATTEMPTS;
        AtomicInteger count = attempts.get(email.trim().toLowerCase());
        return Math.max(0, MAX_ATTEMPTS - (count == null ? 0 : count.get()));
    }
}
