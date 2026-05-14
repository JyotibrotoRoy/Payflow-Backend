package notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationWorker {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-status", groupId = "notification-group")
    public void processPaymentNotification(String payload) {
        System.out.println("\n[NOTIFICATION] Received payment status update. Parsing payload...");

        try {
            JsonNode data = objectMapper.readTree(payload);
            String email = data.get("email").asText();
            String status = data.get("status").asText();
            String orderId = data.get("orderId").asText();

            if("SUCCESS".equalsIgnoreCase(status)) {
                sendEmail(email,"Payment Successful - PayFlow", "Your payment for order " + orderId + " was successful. Thank you!");
            } else if ("FAILED".equalsIgnoreCase(status)) {
                sendEmail(email,"Payment Failed - Action Required", "Your payment for order " + orderId + " failed. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("[NOTIFICATION-ERROR] Failed to process notification: " + e.getMessage());        }
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@payflow.com");
        message.setTo(to);
        message.setText(text);
        message.setSubject(subject);

        mailSender.send(message);
        System.out.println("[NOTIFICATION] Email successfully dispatched to: " + to);
    }
}
