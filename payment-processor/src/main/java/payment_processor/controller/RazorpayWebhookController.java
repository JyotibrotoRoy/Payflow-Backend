package payment_processor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import payment_processor.service.WebhookService;

@Controller
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = true) String signature
    ) {
        System.out.println("[WEBHOOK] Received incoming POST request from Razorpay");

        try {
            // In production: verify signature here first

            // Hand off the heavy lifting to the Service layer
            webhookService.processRazorpayEvent(rawPayload, signature);

            // Immediately return 200 OK so Razorpay drops the connection
            return ResponseEntity.ok("Webhook received and queued.");

        } catch (Exception e) {
            System.err.println("CRITICAL WEBHOOK FAILURE");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
