package com.cargo.controller;

import com.cargo.entity.Customer;
import com.cargo.entity.User;
import com.cargo.enums.ShipmentStatus;
import com.cargo.service.CustomerService;
import com.cargo.service.ShipmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private CustomerService customerService;

    // ===== Admin Dashboard =====
    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != com.cargo.enums.Role.ADMIN) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("totalShipments", shipmentService.getAllShipments().size());
        model.addAttribute("booked", shipmentService.countByStatus(ShipmentStatus.BOOKED));
        model.addAttribute("inTransit", shipmentService.countByStatus(ShipmentStatus.IN_TRANSIT));
        model.addAttribute("delivered", shipmentService.countByStatus(ShipmentStatus.DELIVERED));
        model.addAttribute("totalRevenue", shipmentService.getTotalRevenue());
        return "admin-dashboard";
    }

    // ===== Staff Dashboard =====
    @GetMapping("/staff/dashboard")
    public String staffDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != com.cargo.enums.Role.STAFF) return "redirect:/login";

        List<com.cargo.entity.Shipment> assigned = shipmentService.getShipmentsByStaff(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("assignedShipments", assigned);
        model.addAttribute("allShipments", shipmentService.getAllShipments());
        return "staff-dashboard";
    }

    // ===== Customer Dashboard =====
    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() != com.cargo.enums.Role.CUSTOMER) return "redirect:/login";

        Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
        model.addAttribute("user", user);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            model.addAttribute("customer", customer);
            model.addAttribute("myShipments", shipmentService.getShipmentsByCustomer(customer.getId()));
        }
        return "customer-dashboard";
    }
}
