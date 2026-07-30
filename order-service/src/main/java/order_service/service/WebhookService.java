package order_service.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import order_service.entity.Order;
import order_service.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public void processRazorpayEvent(String rawPayload, String signature) {
        try {
            // 1. Cryptographic Verification
            boolean isValid = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
            if (!isValid) {
                throw new SecurityException("CRITICAL: Invalid webhook signature. Potential spoofing attack dropped.");
            }

            // 2. Safe Parsing
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("event").asText("");

            if ("payment.authorized".equals(eventType) || "payment.captured".equals(eventType)) {

                // USE .path() INSTEAD OF .get()
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");

                // 3. The Guard Clause
                if (!paymentEntity.hasNonNull("order_id")) {
                    System.out.println("[WEBHOOK] Ignored event: Payload contains no order_id. (Likely a Dashboard Test Ping or direct payment)");
                    return;
                }

                String rzpOrderId = paymentEntity.path("order_id").asText();
                String rzpPaymentId = paymentEntity.path("id").asText();

                // 4. Database Lookup
                Order order = orderRepository.findByRazorpayOrderId(rzpOrderId)
                        .orElseThrow(() -> new RuntimeException("Unknown Razorpay Order: " + rzpOrderId));

                // 5. Emit to Kafka
                ObjectNode internalEvent = objectMapper.createObjectNode();
                internalEvent.put("internalOrderId", order.getId().toString());
                internalEvent.put("razorpayOrderId", rzpOrderId);
                internalEvent.put("razorpayPaymentId", rzpPaymentId);
                internalEvent.put("amount", order.getAmount().doubleValue());

                kafkaTemplate.send("payment-events", internalEvent.toString());
                System.out.println("[WEBHOOK-SERVICE] Event queued for internal Order: " + order.getId());
            }
        } catch (Exception e) {
            System.err.println("CRITICAL WEBHOOK FAILURE:");
            e.printStackTrace();
            throw new RuntimeException("Failed to process webhook payload", e);
        }
    }
}