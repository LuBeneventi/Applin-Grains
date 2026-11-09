package com.pedidos360.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.audit.dto.AuditEventResponseDto;
import com.pedidos360.audit.dto.AuditTimelineEvent;
import com.pedidos360.audit.entity.AuditEvent;
import com.pedidos360.audit.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public static final String TOPIC_AUDIT_TIMELINE = "audit.timeline";

    private final AuditEventRepository auditEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Procesa un evento canónico recibido desde 'orders.events',
     * guarda el registro de auditoría ("quién, qué, cuándo, desde dónde") y
     * reenvía la línea de tiempo hacia 'audit.timeline'.
     */
    public AuditEvent processOrderEvent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            String eventId = root.path("eventId").asText("");
            if (auditEventRepository.existsByEventId(eventId)) {
                log.info("Evento de auditoría ya procesado previamente (idempotencia): {}", eventId);
                return null;
            }

            String eventType = root.path("eventType").asText("UnknownEvent");
            Long orderId = root.path("orderId").asLong(0);
            String orderNumber = root.path("orderNumber").asText("PED-" + orderId);
            String actorId = root.path("clienteId").asText("ANONYMOUS");
            String actorName = root.path("clienteNombre").asText("");
            String actorEmail = root.path("clienteEmail").asText("");
            String previousState = root.hasNonNull("previousStatus") ? root.get("previousStatus").asText() : null;
            String newState = root.hasNonNull("currentStatus") ? root.get("currentStatus").asText() : null;
            String source = root.path("metadata").path("source").asText("ms-pedidos360-orders");

            Instant timestamp = root.hasNonNull("timestamp")
                    ? Instant.parse(root.get("timestamp").asText())
                    : Instant.now();

            String actionSummary = generateActionSummary(eventType, orderNumber, actorName, previousState, newState);

            AuditEvent auditEvent = new AuditEvent(
                    eventId,
                    eventType,
                    "ORDER",
                    String.valueOf(orderId),
                    actorId,
                    actorName,
                    actorEmail,
                    actionSummary,
                    previousState,
                    newState,
                    source,
                    rawJson,
                    timestamp
            );

            AuditEvent saved = auditEventRepository.save(auditEvent);

            // Publicar en audit.timeline
            AuditTimelineEvent timelineEvent = new AuditTimelineEvent(
                    saved.getEventId(),
                    saved.getEventType(),
                    saved.getEntityType(),
                    saved.getEntityId(),
                    saved.getActorId(),
                    saved.getActorName(),
                    saved.getActorEmail(),
                    saved.getActionSummary(),
                    saved.getPreviousState(),
                    saved.getNewState(),
                    saved.getSource(),
                    saved.getTimestamp()
            );

            kafkaTemplate.send(TOPIC_AUDIT_TIMELINE, saved.getEntityId(), timelineEvent);
            log.info("Auditoría registrada y publicada en audit.timeline: eventId={}, entityId={}, action={}",
                    eventId, saved.getEntityId(), actionSummary);

            return saved;

        } catch (Exception e) {
            log.error("Error al procesar evento de auditoría: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando evento de auditoría", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponseDto> getTimelineByOrder(Long orderId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampAsc("ORDER", String.valueOf(orderId))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponseDto> searchAuditEvents(String usuario, String eventType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (usuario != null && !usuario.isBlank()) {
                String pattern = "%" + usuario.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorId")), pattern),
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("actorEmail")), pattern)
                ));
            }

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditEventRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    private String generateActionSummary(String eventType, String orderNumber, String actorName, String prev, String next) {
        String who = (actorName != null && !actorName.isBlank()) ? actorName : "Usuario/Sistema";
        return switch (eventType) {
            case "OrderCreated" -> who + " creó el pedido " + orderNumber + " en estado CREADO";
            case "OrderAccepted" -> who + " aceptó el pedido " + orderNumber;
            case "OrderPreparing" -> "Pedido " + orderNumber + " pasó a preparación en cocina/barra";
            case "OrderDispatched" -> "Pedido " + orderNumber + " fue despachado para entrega";
            case "OrderDelivered" -> "Pedido " + orderNumber + " entregado exitosamente al cliente";
            case "OrderCancelled" -> "Pedido " + orderNumber + " fue cancelado";
            default -> who + " actualizó estado de " + orderNumber + " de " + prev + " a " + next;
        };
    }

    private AuditEventResponseDto mapToDto(AuditEvent entity) {
        return new AuditEventResponseDto(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getActorId(),
                entity.getActorName(),
                entity.getActorEmail(),
                entity.getActionSummary(),
                entity.getPreviousState(),
                entity.getNewState(),
                entity.getSource(),
                entity.getPayloadJson(),
                entity.getTimestamp()
        );
    }
}
