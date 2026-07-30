package notification_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscordNotificationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    @KafkaListener(topics = "payment-status", groupId = "discord-group")
    public void sendToDiscord(String message) {
        try {
            // 1. Extract the data
            JsonNode payload = objectMapper.readTree(message);
            String orderId = payload.path("orderId").asText("Unknown");
            String status = payload.path("status").asText("UNKNOWN");
            double amount = payload.path("amount").asDouble(0.0);

            // 2. Set dynamic colors based on status
            int embedColor = status.equalsIgnoreCase("SUCCESS") ? 3066993 : 15158332;

            // 3. Construct the Embed Object
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", "💳 Payment Processed via PayFlow");
            embed.put("color", embedColor);
            embed.put("timestamp", Instant.now().toString());

            // 4. Create Fields for the Embed
            List<Map<String, Object>> fields = new ArrayList<>();

            fields.add(createField("Order ID", "`" + orderId + "`", false));
            fields.add(createField("Amount", "₹" + amount, true));
            fields.add(createField("Status", "**" + status + "**", true));

            embed.put("fields", fields);

            // 5. Wrap in the main payload
            Map<String, Object> discordPayload = new HashMap<>();
            List<Map<String, Object>> embedsList = new ArrayList<>();
            embedsList.add(embed);
            discordPayload.put("embeds", embedsList);

            // 6. Fire the HTTP POST
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(discordPayload, headers);

            restTemplate.postForEntity(discordWebhookUrl, request, String.class);
            System.out.println("DISCORD LOG: Pushed embed update for order " + orderId);

        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }

    // Helper method to keep field creation clean
    private Map<String, Object> createField(String name, String value, boolean inline) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }
}