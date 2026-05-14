package notification_service.controller;

import notification_service.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // The endpoint the frontend calls to start streaming
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> streamEvents() {

        SseEmitter emitter = auditService.createConnection();

        try {
            emitter.send(SseEmitter.event().name("init").data("Connection Established"));
        } catch (IOException e) {
            emitter.complete();
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

}
