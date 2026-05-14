package notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final List<SseEmitter> activeEmitters = new CopyOnWriteArrayList<>();

    // Called by the Controller when a frontend connects
    public SseEmitter createConnection() {
        SseEmitter emitter = new SseEmitter(600000L); // 10-minute timeout
        this.activeEmitters.add(emitter);

        // Cleanup dead connections when users close the browser tab
        emitter.onCompletion(() -> this.activeEmitters.remove(emitter));
        emitter.onTimeout(() -> this.activeEmitters.remove(emitter));
        emitter.onError((e) -> this.activeEmitters.remove(emitter));

        return emitter;
    }

    // Listens to Kafka and pushes the message to all connected browsers
    @KafkaListener(topics = "payment-status", groupId = "audit-group")
    public void consumeAndBroadcast(String message) {
        System.out.println("AUDIT LOG: " + message);

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        this.activeEmitters.forEach(emitter -> {
            try {
                // Send an event named "payment" containing the JSON string
                emitter.send(SseEmitter.event().name("payment").data(message));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove connections that threw an exception
        this.activeEmitters.removeAll(deadEmitters);
    }
}
