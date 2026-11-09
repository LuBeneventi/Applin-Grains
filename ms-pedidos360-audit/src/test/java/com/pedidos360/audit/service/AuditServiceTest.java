package com.pedidos360.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.audit.entity.AuditEvent;
import com.pedidos360.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("Debe procesar evento OrderCreated, guardar en BD y publicar en audit.timeline")
    void testProcessOrderCreated() {
        String json = """
        {
          "eventId": "evt-001",
          "eventType": "OrderCreated",
          "orderId": 50,
          "orderNumber": "PED-50",
          "clienteId": "CLI-888",
          "clienteNombre": "Ana López",
          "clienteEmail": "ana@example.com",
          "currentStatus": "CREADO",
          "timestamp": "2026-11-09T14:00:00Z",
          "metadata": { "source": "ms-pedidos360-orders" }
        }
        """;

        when(auditEventRepository.existsByEventId("evt-001")).thenReturn(false);
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditEvent result = auditService.processOrderEvent(json);

        assertNotNull(result);
        assertEquals("OrderCreated", result.getEventType());
        assertEquals("50", result.getEntityId());
        assertEquals("Ana López", result.getActorName());
        assertTrue(result.getActionSummary().contains("creó el pedido PED-50"));

        verify(auditEventRepository).save(any(AuditEvent.class));
        verify(kafkaTemplate).send(eq("audit.timeline"), eq("50"), any());
    }

    @Test
    @DisplayName("Debe ignorar evento si ya existe por eventId (Idempotencia)")
    void testIgnoreDuplicateEvent() {
        String json = """
        {
          "eventId": "evt-duplicate",
          "eventType": "OrderAccepted",
          "orderId": 50
        }
        """;

        when(auditEventRepository.existsByEventId("evt-duplicate")).thenReturn(true);

        AuditEvent result = auditService.processOrderEvent(json);

        assertNull(result);
        verify(auditEventRepository, never()).save(any(AuditEvent.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Debe devolver el timeline ordenado de un pedido")
    void testGetTimelineByOrder() {
        AuditEvent event1 = new AuditEvent("e1", "OrderCreated", "ORDER", "50", "u1", "Ana", "a@a.cl", "Creó", null, "CREADO", "orders", "{}", java.time.Instant.now());
        AuditEvent event2 = new AuditEvent("e2", "OrderAccepted", "ORDER", "50", "admin", "Admin", "adm@a.cl", "Aceptó", "CREADO", "ACEPTADO", "orders", "{}", java.time.Instant.now());

        when(auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampAsc("ORDER", "50"))
                .thenReturn(List.of(event1, event2));

        var timeline = auditService.getTimelineByOrder(50L);

        assertEquals(2, timeline.size());
        assertEquals("OrderCreated", timeline.get(0).getEventType());
        assertEquals("OrderAccepted", timeline.get(1).getEventType());
    }
}
