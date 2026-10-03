package com.cargo.controller;

import com.cargo.entity.Customer;
import com.cargo.entity.User;
import com.cargo.enums.Role;
import com.cargo.service.CustomerService;
import com.cargo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    // ===== Home -> redirect to login =====
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // ===== Login =====
    // POST /login is now handled entirely by Spring Security's formLogin
    // (see SecurityConfig, LoginSuccessHandler, LoginFailureHandler).
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ===== Register (Customer self-registration) =====
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute User user,
                                   @RequestParam String address,
                                   Model model) {
        try {
            user.setRole(Role.CUSTOMER);
            User savedUser = userService.registerUser(user);

            // Auto-create linked Customer profile
            Customer customer = new Customer();
            customer.setName(savedUser.getFullName());
            customer.setPhone(savedUser.getPhone());
            customer.setEmail(savedUser.getEmail());
            customer.setAddress(address);
            customer.setUser(savedUser);
            customerService.saveCustomer(customer);

            model.addAttribute("success", "Registration successful! Please login.");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // Logout is now handled by Spring Security (GET /logout, see SecurityConfig)
}
