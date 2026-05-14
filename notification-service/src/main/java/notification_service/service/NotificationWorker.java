package notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class NotificationWorker {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    @Value("${app.email.from}")
    private String fromEmail;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-status", groupId = "notification-group")
    public void handlePaymentNotification(String message) {
        System.out.println("PROCESSING KAFKA EVENT: " + message);

        try {
            // 1. Parse the incoming Kafka JSON string
            JsonNode payload = objectMapper.readTree(message);

            // 2. Extract the exact variables dynamically
            String recipientEmail = payload.get("email").asText();
            String orderId = payload.get("orderId").asText();
            String status = payload.get("status").asText();

            // 3. Build a professional HTML email template
            String htmlBody = """
                    <div style="font-family: Arial, sans-serif; padding: 20px;">
                        <h2>Payment %s</h2>
                        <p>Thank you for your order. Your payment has been successfully processed.</p>
                        <p><strong>Order ID:</strong> %s</p>
                    </div>
                    """.formatted(status, orderId);

            String resendPayload = """
                    {
                      "from": "%s",
                      "to": ["%s"],
                      "subject": "PayFlow Update: Order %s",
                      "html": "%s"
                    }
                    """.formatted(fromEmail, recipientEmail, orderId, htmlBody.replace("\"", "\\\"").replace("\n", ""));

            // 5. Fire the HTTP Request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(resendPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("SUCCESS: Email delivered to " + recipientEmail);
            } else {
                System.err.println("RESEND API ERROR: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            System.err.println("FATAL ERROR processing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
