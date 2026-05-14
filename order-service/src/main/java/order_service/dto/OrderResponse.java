package order_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderResponse {
    private UUID orderId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String message;
}
