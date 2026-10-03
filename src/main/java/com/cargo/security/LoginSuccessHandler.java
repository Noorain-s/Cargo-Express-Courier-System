package com.cargo.security;

import com.cargo.entity.LoginActivity;
import com.cargo.entity.User;
import com.cargo.repository.LoginActivityRepository;
import com.cargo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginActivityRepository loginActivityRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        String email = authentication.getName();
        loginAttemptService.loginSucceeded(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + email));

        // Keep populating the same session attributes the rest of the app already reads
        HttpSession session = request.getSession();
        session.setAttribute("loggedInUser", user);
        session.setAttribute("role", user.getRole().name());

        loginActivityRepository.save(new LoginActivity(
                user, user.getEmail(), user.getRole().name(), LocalDateTime.now()));

        String redirectUrl;
        switch (user.getRole()) {
            case ADMIN: redirectUrl = "/admin/dashboard"; break;
            case STAFF: redirectUrl = "/staff/dashboard"; break;
            case CUSTOMER: redirectUrl = "/customer/dashboard"; break;
            default: redirectUrl = "/login"; break;
        }
        response.sendRedirect(redirectUrl);
    }
}
