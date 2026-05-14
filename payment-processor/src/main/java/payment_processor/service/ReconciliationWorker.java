package payment_processor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import payment_processor.entity.Order;
import payment_processor.entity.OrderStatus;
import payment_processor.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationWorker {

    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0/15 * * * *")
    public void sweepStaleOrders() {
        System.out.println("\n[RECONCILIATION] Waking up. Hunting for stale PENDING orders...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        List<Order> staleOrders = orderRepository.findStalePendingOrders(cutoffTime);

        if (staleOrders.isEmpty()) {
            System.out.println("[RECONCILIATION] Database clean. Going back to sleep.");
            return;
        }

        System.out.println("[RECONCILIATION] Found " + staleOrders.size() + " stale orders. Querying Razorpay API...");

        for(Order localOrder: staleOrders) {
            try {
                com.razorpay.Order razorpayOrder = razorpayClient.orders.fetch(localOrder.getRazorpayOrderId());

                String trueStatus = razorpayOrder.get("status");

                if ("paid".equalsIgnoreCase(trueStatus)) {
                    System.out.println("[RECONCILIATION] Webhook dropped! Razorpay confirmed Order " + localOrder.getId() + " was PAID. Forcing update.");

                    localOrder.setStatus(OrderStatus.SUCCESS);
                    orderRepository.save(localOrder);

                    ObjectNode payload = objectMapper.createObjectNode();

                    payload.put("orderId", localOrder.getId().toString());
                    payload.put("status", "SUCCESS");
                    payload.put("email", localOrder.getEmail());
                    payload.put("amount", localOrder.getAmount().doubleValue());

                    kafkaTemplate.send("payment-status", payload.toString());

                } else if ("attempted".equalsIgnoreCase(trueStatus) && localOrder.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                    System.out.println("[RECONCILIATION] Order " + localOrder.getId() + " abandoned. Marking FAILED.");
                    localOrder.setStatus(OrderStatus.FAILED);
                    orderRepository.save(localOrder);
                }

            } catch (Exception e) {
                System.err.println("CRITICAL WEBHOOK FAILURE: at Reconciliation");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}