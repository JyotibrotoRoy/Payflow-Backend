package order_service.controller;

import lombok.RequiredArgsConstructor;
import order_service.dto.OrderRequest;
import order_service.service.OrderManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderManagementService orderManagementService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody OrderRequest request) {
        String razorpayOrderId = orderManagementService.createOrder(request);

        return ResponseEntity.ok(Map.of("razorpayOrderId", razorpayOrderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody Map<String, String> payload) {
        // The frontend sends the 3 Razorpay signature variables here
        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpaySignature = payload.get("razorpay_signature");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return ResponseEntity.badRequest().body("Missing required Razorpay parameters in request body.");
        }

        try {
            orderManagementService.verifyPaymentAndPublishEvent(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            return ResponseEntity.ok("Payment verified successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Verification failed: " + e.getMessage());
        }
    }

}
