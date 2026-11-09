package com.pedidos360.audit.controller;

import com.pedidos360.audit.dto.AuditEventResponseDto;
import com.pedidos360.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Endpoints de Auditoría y Línea de Tiempo de Eventos (Kafka)")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/orders/{orderId}/timeline")
    @Operation(summary = "Obtener línea de tiempo de un pedido",
               description = "Devuelve el historial cronológico y completo de eventos ocurridos sobre un pedido.")
    public ResponseEntity<List<AuditEventResponseDto>> getOrderTimeline(@PathVariable Long orderId) {
        return ResponseEntity.ok(auditService.getTimelineByOrder(orderId));
    }

    @GetMapping("/events")
    @Operation(summary = "Buscar y filtrar eventos de auditoría",
               description = "Permite filtrar por usuario/actor, tipo de evento y rango de fechas con paginación.")
    public ResponseEntity<Page<AuditEventResponseDto>> searchEvents(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.searchAuditEvents(usuario, eventType, from, to, pageRequest));
    }
}
