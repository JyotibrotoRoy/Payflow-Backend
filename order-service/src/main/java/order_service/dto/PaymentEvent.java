package order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String internalOrderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
}
