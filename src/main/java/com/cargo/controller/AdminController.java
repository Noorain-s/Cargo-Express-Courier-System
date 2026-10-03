package com.cargo.controller;

import com.cargo.entity.User;
import com.cargo.enums.Role;
import com.cargo.repository.LoginActivityRepository;
import com.cargo.service.ShipmentService;
import com.cargo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private LoginActivityRepository loginActivityRepository;

    // ===== Manage Staff =====
    @GetMapping("/staff")
    public String manageStaff(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) return "redirect:/login";

        model.addAttribute("staffList", userService.getUsersByRole(Role.STAFF));
        model.addAttribute("newStaff", new User());
        return "manage-staff";
    }

    @PostMapping("/staff/add")
    public String addStaff(@ModelAttribute User newStaff, HttpSession session, Model model) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) return "redirect:/login";

        try {
            newStaff.setRole(Role.STAFF);
            userService.registerUser(newStaff);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/delete/{id}")
    public String deleteStaff(@PathVariable Long id, HttpSession session) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) return "redirect:/login";

        userService.deleteUser(id);
        return "redirect:/admin/staff";
    }

    // ===== Reports =====
    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) return "redirect:/login";

        model.addAttribute("totalShipments", shipmentService.getAllShipments().size());
        model.addAttribute("totalRevenue", shipmentService.getTotalRevenue());
        model.addAttribute("booked", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.BOOKED));
        model.addAttribute("pickedUp", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.PICKED_UP));
        model.addAttribute("inTransit", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.IN_TRANSIT));
        model.addAttribute("outForDelivery", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.OUT_FOR_DELIVERY));
        model.addAttribute("delivered", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.DELIVERED));
        model.addAttribute("cancelled", shipmentService.countByStatus(com.cargo.enums.ShipmentStatus.CANCELLED));
        return "reports";
    }

    // ===== Login Activity (who's logged in, and when) =====
    @GetMapping("/login-activity")
    public String loginActivity(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) return "redirect:/login";

        model.addAttribute("logins", loginActivityRepository.findAllByOrderByLoginTimeDesc());
        return "login-activity";
    }
}
