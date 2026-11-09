package com.pedidos360.orders.event.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Evento canónico de dominio publicado en el tópico Kafka 'orders.events'.
 * Tipos de eventos mínimos:
 * - OrderCreated
 * - OrderAccepted
 * - OrderPreparing
 * - OrderDispatched
 * - OrderDelivered
 * - OrderCancelled
 */
public class OrderDomainEvent {

    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderNumber;
    private String clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private String clienteDireccion;
    private String previousStatus;
    private String currentStatus;
    private BigDecimal total;
    private List<OrderDomainItem> items;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public OrderDomainEvent() {
    }

    public OrderDomainEvent(String eventId, String eventType, Long orderId, String orderNumber,
                            String clienteId, String clienteNombre, String clienteEmail,
                            String clienteDireccion, String previousStatus, String currentStatus,
                            BigDecimal total, List<OrderDomainItem> items, Instant timestamp,
                            Map<String, Object> metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.clienteDireccion = clienteDireccion;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.total = total;
        this.items = items;
        this.timestamp = timestamp;
        this.metadata = metadata;
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<OrderDomainItem> getItems() {
        return items;
    }

    public void setItems(List<OrderDomainItem> items) {
        this.items = items;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
