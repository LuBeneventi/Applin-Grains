package cl.duoc.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Endpoints del BFF para el módulo de Auditoría (timeline de eventos Kafka).
 * Ruta frontend: /audit (Component: AuditComponent, Roles: Admin)
 */
@RestController
public class AuditController {

    private final RestClient auditServiceClient;

    public AuditController(@Qualifier("auditServiceClient") RestClient auditServiceClient) {
        this.auditServiceClient = auditServiceClient;
    }

    @GetMapping("/api/audit/orders/{orderId}/timeline")
    public ResponseEntity<String> getOrderTimeline(@PathVariable Long orderId) {
        return auditServiceClient
                .get()
                .uri("/api/audit/orders/" + orderId + "/timeline")
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/api/audit/events")
    public ResponseEntity<String> searchEvents(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/audit/events" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return auditServiceClient
                .get()
                .uri(targetUri)
                .retrieve()
                .toEntity(String.class);
    }
}
