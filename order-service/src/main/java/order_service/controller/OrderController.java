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

}
