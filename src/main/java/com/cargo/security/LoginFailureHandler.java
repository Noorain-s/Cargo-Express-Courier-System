package com.cargo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        String email = request.getParameter("email");
        String message;

        if (exception instanceof LockedException) {
            message = "Too many failed attempts. Account locked for 15 minutes.";
        } else {
            loginAttemptService.loginFailed(email);
            int remaining = loginAttemptService.attemptsRemaining(email);
            message = remaining > 0
                    ? "Invalid email or password. " + remaining + " attempt(s) left before temporary lockout."
                    : "Too many failed attempts. Account locked for 15 minutes.";
        }

        response.sendRedirect("/login?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
