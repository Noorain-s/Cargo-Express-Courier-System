package com.cargo.controller;

import com.cargo.entity.Customer;
import com.cargo.entity.Payment;
import com.cargo.entity.Shipment;
import com.cargo.entity.User;
import com.cargo.enums.PackageType;
import com.cargo.enums.ShipmentStatus;
import com.cargo.service.CustomerService;
import com.cargo.service.PaymentService;
import com.cargo.service.ShipmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/shipment")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PaymentService paymentService;

    // ===== Booking Form (Customer) =====
    @GetMapping("/book")
    public String bookForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("packageTypes", PackageType.values());
        return "book-shipment";
    }

    @PostMapping("/book")
    public String processBooking(HttpSession session,
                                  @RequestParam String receiverName,
                                  @RequestParam String receiverPhone,
                                  @RequestParam String receiverAddress,
                                  @RequestParam PackageType packageType,
                                  @RequestParam double weightKg,
                                  @RequestParam String paymentMode,
                                  Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
        if (customerOpt.isEmpty()) {
            model.addAttribute("error", "Customer profile not found");
            return "book-shipment";
        }

        Shipment shipment = new Shipment();
        shipment.setSender(customerOpt.get());
        shipment.setReceiverName(receiverName);
        shipment.setReceiverPhone(receiverPhone);
        shipment.setReceiverAddress(receiverAddress);
        shipment.setPackageType(packageType);
        shipment.setWeightKg(weightKg);

        Shipment saved = shipmentService.bookShipment(shipment);

        // Creates the payment record. For CASH this is COD (pending until delivery).
        // For CARD/UPI/ONLINE this creates a real Razorpay order - nothing is marked
        // paid yet until the customer actually completes checkout.
        paymentService.createPayment(saved, paymentMode);

        if ("CASH".equalsIgnoreCase(paymentMode)) {
            return "redirect:/shipment/invoice/" + saved.getId();
        }
        return "redirect:/shipment/pay/" + saved.getId();
    }

    // ===== Real payment checkout page (Razorpay) =====
    @GetMapping("/pay/{id}")
    public String payPage(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Optional<Shipment> shipmentOpt = shipmentService.getShipmentById(id);
        Optional<Payment> paymentOpt = paymentService.getPaymentByShipmentId(id);
        if (shipmentOpt.isEmpty() || paymentOpt.isEmpty()) return "redirect:/customer/dashboard";

        if (user.getRole() == com.cargo.enums.Role.CUSTOMER && !ownsShipment(user, shipmentOpt.get())) {
            return "redirect:/customer/dashboard";
        }

        Payment payment = paymentOpt.get();
        if ("PAID".equals(payment.getPaymentStatus())) {
            return "redirect:/shipment/invoice/" + id;
        }

        model.addAttribute("shipment", shipmentOpt.get());
        model.addAttribute("payment", payment);
        model.addAttribute("razorpayKeyId", paymentService.getRazorpayKeyId());
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("customerEmail", user.getEmail());
        model.addAttribute("simulateMode", paymentService.isSimulateMode());
        return "pay";
    }

    // ===== Called by the browser after Razorpay Checkout succeeds =====
    @PostMapping("/payment/verify")
    @ResponseBody
    public java.util.Map<String, Object> verifyPayment(@RequestParam Long shipmentId,
                                                         @RequestParam String razorpay_payment_id,
                                                         @RequestParam String razorpay_order_id,
                                                         @RequestParam String razorpay_signature,
                                                         HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        Optional<Payment> paymentOpt = paymentService.getPaymentByShipmentId(shipmentId);

        if (user == null || paymentOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Session expired or payment not found");
            return response;
        }

        // Make sure this payment actually belongs to the logged-in customer
        Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
        Shipment linkedShipment = paymentOpt.get().getShipment();
        boolean owns = customerOpt.isPresent() && linkedShipment != null
                && linkedShipment.getSender().getId().equals(customerOpt.get().getId());
        if (user.getRole() == com.cargo.enums.Role.CUSTOMER && !owns) {
            response.put("success", false);
            response.put("message", "Not authorized");
            return response;
        }

        boolean verified = paymentService.verifyAndMarkPaid(
                paymentOpt.get(), razorpay_payment_id, razorpay_order_id, razorpay_signature);

        response.put("success", verified);
        response.put("redirect", "/shipment/invoice/" + shipmentId);
        return response;
    }

    // ===== Called by the browser's mock checkout modal (payment.simulate=true only) =====
    // No signature verification happens here, since nothing real was ever contacted -
    // this just lets the rest of the app (invoice, dashboards) see a completed payment
    // while the real Razorpay account is still pending KYC activation.
    @PostMapping("/payment/mock-confirm")
    @ResponseBody
    public java.util.Map<String, Object> mockConfirmPayment(@RequestParam Long shipmentId,
                                                              HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        Optional<Payment> paymentOpt = paymentService.getPaymentByShipmentId(shipmentId);

        if (user == null || paymentOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Session expired or payment not found");
            return response;
        }

        Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
        Shipment linkedShipment = paymentOpt.get().getShipment();
        boolean owns = customerOpt.isPresent() && linkedShipment != null
                && linkedShipment.getSender().getId().equals(customerOpt.get().getId());
        if (user.getRole() == com.cargo.enums.Role.CUSTOMER && !owns) {
            response.put("success", false);
            response.put("message", "Not authorized");
            return response;
        }

        boolean confirmed = paymentService.simulatePaymentSuccess(paymentOpt.get());
        response.put("success", confirmed);
        response.put("redirect", "/shipment/invoice/" + shipmentId);
        return response;
    }

    // ===== Called by the browser if Razorpay Checkout fails or is dismissed =====
    @PostMapping("/payment/failed")
    @ResponseBody
    public java.util.Map<String, Object> paymentFailed(@RequestParam Long shipmentId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        paymentService.getPaymentByShipmentId(shipmentId).ifPresent(paymentService::markFailed);
        response.put("success", false);
        return response;
    }

    // ===== Invoice View =====
    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Optional<Shipment> shipmentOpt = shipmentService.getShipmentById(id);
        if (shipmentOpt.isEmpty()) return "redirect:/customer/dashboard";

        if (user.getRole() == com.cargo.enums.Role.CUSTOMER && !ownsShipment(user, shipmentOpt.get())) {
            return "redirect:/customer/dashboard";
        }

        Optional<Payment> paymentOpt = paymentService.getPaymentByShipmentId(id);

        model.addAttribute("shipment", shipmentOpt.get());
        model.addAttribute("payment", paymentOpt.orElse(null));
        return "invoice";
    }

    private boolean ownsShipment(User user, Shipment shipment) {
        Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
        return customerOpt.isPresent() && shipment.getSender() != null
                && shipment.getSender().getId().equals(customerOpt.get().getId());
    }

    // ===== Tracking Page (Customer sees own, Staff/Admin see all) =====
    @GetMapping("/track")
    public String trackShipments(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (user.getRole().name().equals("CUSTOMER")) {
            Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
            model.addAttribute("shipments",
                    customerOpt.map(c -> shipmentService.getShipmentsByCustomer(c.getId())).orElse(java.util.List.of()));
        } else {
            model.addAttribute("shipments", shipmentService.getAllShipments());
        }
        model.addAttribute("statuses", ShipmentStatus.values());
        model.addAttribute("user", user);
        return "track-shipment";
    }

    // ===== Update Status (Staff/Admin only) =====
    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam Long shipmentId,
                                @RequestParam ShipmentStatus newStatus,
                                HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (user.getRole() == com.cargo.enums.Role.CUSTOMER) return "redirect:/shipment/track";

        shipmentService.updateStatus(shipmentId, newStatus);
        return "redirect:/shipment/track";
    }

    // ===== Cancel Shipment (owner customer, or staff/admin) =====
    @PostMapping("/cancel/{id}")
    public String cancelShipment(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (user.getRole() == com.cargo.enums.Role.CUSTOMER) {
            Optional<Customer> customerOpt = customerService.getCustomerByUserId(user.getId());
            Optional<Shipment> shipmentOpt = shipmentService.getShipmentById(id);
            boolean ownsShipment = customerOpt.isPresent() && shipmentOpt.isPresent()
                    && shipmentOpt.get().getSender().getId().equals(customerOpt.get().getId());
            if (!ownsShipment) return "redirect:/shipment/track";
        }

        shipmentService.cancelShipment(id);
        return "redirect:/shipment/track";
    }
}
