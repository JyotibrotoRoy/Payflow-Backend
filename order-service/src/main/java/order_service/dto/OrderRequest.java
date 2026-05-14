package order_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {
    private String customerName;
    private String email;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
