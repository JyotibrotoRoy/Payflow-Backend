package notification_service.controller;

import notification_service.service.AuditService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    public SseEmitter streamEvents() {
        return auditService.createConnection();
    }
}
