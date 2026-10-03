package com.cargo.service;

import com.cargo.entity.Payment;
import com.cargo.entity.Shipment;
import com.cargo.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // See application.properties: true while Razorpay account KYC is pending.
    // Skips the real API call and lets the pay page use a local mock checkout instead.
    @Value("${payment.simulate:false}")
    private boolean simulateMode;

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public boolean isSimulateMode() {
        return simulateMode;
    }

    /**
     * Creates the local payment record for a shipment.
     * CASH -> Cash on Delivery: stays PENDING until staff/admin marks it collected.
     * CARD / UPI / ONLINE, simulateMode=false -> a real Razorpay order is created;
     * status stays PENDING until Razorpay confirms the transaction and the signature
     * is verified.
     * CARD / UPI / ONLINE, simulateMode=true -> no external call is made; a local
     * placeholder order id is generated so the pay page's mock checkout can run.
     */
    public Payment createPayment(Shipment shipment, String mode) {
        Payment payment = new Payment();
        payment.setShipment(shipment);
        payment.setAmount(shipment.getCost());
        payment.setMode(mode);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentStatus("PENDING");

        if (!"CASH".equalsIgnoreCase(mode)) {
            if (simulateMode) {
                payment.setRazorpayOrderId("order_sim_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14));
            } else {
                try {
                    RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                    JSONObject orderRequest = new JSONObject();
                    // Razorpay expects amount in paise (smallest currency unit)
                    orderRequest.put("amount", Math.round(shipment.getCost() * 100));
                    orderRequest.put("currency", "INR");
                    orderRequest.put("receipt", "shipment_" + shipment.getId());
                    Order order = razorpay.orders.create(orderRequest);
                    payment.setRazorpayOrderId(order.get("id"));
                } catch (Exception e) {
                    // Order creation failed (bad keys, no network, etc.) - leave it FAILED,
                    // the pay page will show an error instead of a fake "success"
                    payment.setPaymentStatus("FAILED");
                }
            }
        }

        return paymentRepository.save(payment);
    }

    /**
     * Simulated-mode confirmation: used only when payment.simulate=true and Razorpay's
     * real checkout/signature flow is bypassed. No external verification happens here -
     * this exists purely so the rest of the app (invoice, dashboard, admin analytics)
     * still has a realistic PAID payment to work with during demos while KYC is pending.
     */
    public boolean simulatePaymentSuccess(Payment payment) {
        String fakePaymentId = "pay_sim_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        payment.setPaymentStatus("PAID");
        payment.setRazorpayPaymentId(fakePaymentId);
        paymentRepository.save(payment);
        return true;
    }

    /**
     * Verifies the signature Razorpay's checkout returns after a real payment attempt.
     * Only marks PAID if the signature genuinely matches - this is what makes the
     * payment "real" instead of a fake demo confirmation.
     */
    public boolean verifyAndMarkPaid(Payment payment, String razorpayPaymentId,
                                      String razorpayOrderId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (valid) {
                payment.setPaymentStatus("PAID");
                payment.setRazorpayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);
                return true;
            } else {
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
                return false;
            }
        } catch (Exception e) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
            return false;
        }
    }

    public void markFailed(Payment payment) {
        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);
    }

    // Staff/Admin confirms cash was physically collected on delivery
    public void markCashCollected(Payment payment) {
        payment.setPaymentStatus("PAID");
        paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentByShipmentId(Long shipmentId) {
        return paymentRepository.findByShipmentId(shipmentId);
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public java.util.List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
