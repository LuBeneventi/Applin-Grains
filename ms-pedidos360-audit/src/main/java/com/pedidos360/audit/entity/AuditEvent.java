package com.pedidos360.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro de auditoría/trazabilidad ("quién, qué, cuándo, desde dónde").
 * Almacena la línea de tiempo de eventos del dominio procesados desde Kafka.
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_user", columnList = "actorId"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String eventType; // OrderCreated, OrderAccepted, OrderPreparing, etc.

    @Column(nullable = false, length = 32)
    private String entityType; // ORDER, PRODUCT, PAYMENT

    @Column(nullable = false, length = 64)
    private String entityId; // "1024", "PED-1024"

    @Column(length = 64)
    private String actorId; // clienteId o usuario que ejecutó la acción ("quién")

    @Column(length = 128)
    private String actorName; // Nombre del actor

    @Column(length = 128)
    private String actorEmail;

    @Column(nullable = false, length = 255)
    private String actionSummary; // Descripción legible del "qué"

    @Column(length = 64)
    private String previousState; // ej: CREADO

    @Column(length = 64)
    private String newState; // ej: ACEPTADO

    @Column(length = 64)
    private String source; // "desde dónde": ej ms-pedidos360-orders

    @Column(columnDefinition = "TEXT")
    private String payloadJson; // Detalle completo en formato JSON

    @Column(nullable = false)
    private Instant timestamp; // "cuándo"

    public AuditEvent() {
    }

    public AuditEvent(String eventId, String eventType, String entityType, String entityId,
                      String actorId, String actorName, String actorEmail, String actionSummary,
                      String previousState, String newState, String source, String payloadJson,
                      Instant timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorEmail = actorEmail;
        this.actionSummary = actionSummary;
        this.previousState = previousState;
        this.newState = newState;
        this.source = source;
        this.payloadJson = payloadJson;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(String actionSummary) {
        this.actionSummary = actionSummary;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
