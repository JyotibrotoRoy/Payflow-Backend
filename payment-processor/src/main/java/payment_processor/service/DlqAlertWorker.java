package payment_processor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class DlqAlertWorker {

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    @KafkaListener(topics = "payment-events-dlt", groupId = "dlq-alert-group")
    public void consumeDlqMessage(String failedMessage) {
        System.out.println("\n[ALARM] Poison Pill detected in DLT. Dispatching alert...");

        String jsonPayload = "{"
                + "\"content\": \"🚨 **CRITICAL: UNPROCESSED PAYMENT EVENT** 🚨\\n"
                + "**Topic:** `payment-events-dlt`\\n"
                + "**Payload:** `" + failedMessage.replace("\"", "\\\"") + "`\""
                + "}";

        try{
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[ALARM] Alert successfully dispatched to external channel.");
            } else {
                System.err.println("[ALARM] Webhook failed with HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[ALARM-CRITICAL] Failed to execute webhook: " + e.getMessage());
        }
    }
}
