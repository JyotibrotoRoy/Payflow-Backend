package payment_processor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment_processor.entity.Order;
import payment_processor.entity.OrderStatus;
import payment_processor.repository.OrderRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWorker {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "payment-events", groupId = "payment-processor-group")
    public void consumePaymentEvent(String message) throws Exception {
        System.out.println("Picked up raw event from Kafka: " + message);

            JsonNode root = objectMapper.readTree(message);

            if (root.isTextual()) {
                System.out.println("[DEBUG] Double-serialization detected. Unwrapping inner JSON...");
                root = objectMapper.readTree(root.asText());
            }

            JsonNode idNode = root.get("internalOrderId");
            if(idNode == null) idNode = root.get("orderId");
            if(idNode == null) idNode = root.get("id");

            if (idNode == null) {
                throw new IllegalArgumentException("Garbage Message: No valid Order ID found.");
            }

            String orderId = idNode.asText();

            if(!root.has("amount")) {
                throw new IllegalArgumentException("Garbage Message: No amount found.");
            }
            double amount = root.get("amount").asDouble();

            Order order = orderRepository.findById(UUID.fromString(orderId))
                    .orElseThrow(() -> new RuntimeException("Order not found in DB: " + orderId));

            RiskAssessment assessment = evaluateRisk(amount);

            String finalStatus = assessment.isApproved ? "SUCCESS" : "FAILED";
            order.setStatus(OrderStatus.valueOf(finalStatus));
            orderRepository.save(order);

            if(assessment.isApproved) {
                System.out.println("[PAYMENT ENGINE] Result: APPROVED. " + assessment.reason);
            } else {
                System.out.println("[PAYMENT ENGINE] Result: DECLINED. Reason: " + assessment.reason);
            }

            ObjectNode payload = objectMapper.createObjectNode();

            payload.put("orderId", order.getId().toString());
            payload.put("status", finalStatus);
            payload.put("email", order.getEmail());
            payload.put("amount", order.getAmount().doubleValue());
            payload.put("reason", assessment.reason);

            kafkaTemplate.send("payment-status", payload.toString());
    }

    private RiskAssessment evaluateRisk(double amount) {
        if (amount == 9999.00) return new RiskAssessment(false, "BANK_DECLINE: Insufficient Funds");
        if (amount == 8888.00) return new RiskAssessment(false, "BANK_DECLINE: Card Expired");
        if (amount == 7777.00) return new RiskAssessment(false, "GATEWAY_REJECT: Suspected Fraud");
        if (amount == 6666.00) return new RiskAssessment(false, "NETWORK_ERROR: Bank Timeout");

        //2.Hard limit checks
        if (amount > 500000) { // e.g., max ticket size ₹5 Lakhs
            return new RiskAssessment(false, "POLICY_REJECT: Exceeds Maximum Transaction Limit");
        }

        int fraudScore = calculateFraudScore(amount);
        if (fraudScore > 85) {
            return new RiskAssessment(false, "RISK_REJECT: Fraud Score too high (" + fraudScore + "/100)");
        }

        return new RiskAssessment(true, "CLEARED: Risk profile acceptable.");
    }

    private int calculateFraudScore(double amount) {
        int baseScore = (int) (amount / 10000);
        int networkEntropy = new java.util.Random().nextInt(40); // Represents IP/Device risk
        return Math.min(100, baseScore + networkEntropy);
    }

    private static class RiskAssessment {
        boolean isApproved;
        String reason;

        RiskAssessment(boolean isApproved, String reason) {
            this.isApproved = isApproved;
            this.reason = reason;
        }
    }
}
