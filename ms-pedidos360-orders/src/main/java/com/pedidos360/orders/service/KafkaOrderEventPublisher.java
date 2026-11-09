package com.pedidos360.orders.service;

import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.event.kafka.OrderDomainEvent;
import com.pedidos360.orders.event.kafka.OrderDomainItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publicador de eventos canónicos hacia Apache Kafka (tópico: orders.events).
 * Alimenta la auditoría y la reportería en tiempo real.
 */
@Service
public class KafkaOrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    public static final String TOPIC_ORDERS_EVENTS = "orders.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        if (order == null) return;
        publishEvent("OrderCreated", order, null, OrderStatus.CREADO.name());
    }

    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        if (order == null || newStatus == null) return;

        String eventType = switch (newStatus) {
            case CREADO -> "OrderCreated";
            case ACEPTADO -> "OrderAccepted";
            case EN_PREPARACION -> "OrderPreparing";
            case DESPACHADO -> "OrderDispatched";
            case ENTREGADO -> "OrderDelivered";
            case CANCELADO -> "OrderCancelled";
        };

        publishEvent(eventType, order, previousStatus != null ? previousStatus.name() : null, newStatus.name());
    }

    private void publishEvent(String eventType, Order order, String previousStatus, String currentStatus) {
        try {
            String eventId = UUID.randomUUID().toString();
            String key = order.getId() != null ? String.valueOf(order.getId()) : UUID.randomUUID().toString();

            List<OrderDomainItem> items = order.getItems() != null ? order.getItems().stream()
                    .map(it -> new OrderDomainItem(
                            it.getProductoId(),
                            it.getDescripcion(),
                            it.getCantidad(),
                            it.getPrecioUnitario(),
                            it.getSubtotal()
                    )).toList() : List.of();

            OrderDomainEvent event = new OrderDomainEvent(
                    eventId,
                    eventType,
                    order.getId(),
                    "PED-" + order.getId(),
                    order.getClienteId(),
                    order.getClienteNombre(),
                    order.getClienteEmail(),
                    order.getClienteDireccion(),
                    previousStatus,
                    currentStatus,
                    order.getTotal(),
                    items,
                    Instant.now(),
                    Map.of(
                            "source", "ms-pedidos360-orders",
                            "rut", order.getClienteRut() != null ? order.getClienteRut() : ""
                    )
            );

            kafkaTemplate.send(TOPIC_ORDERS_EVENTS, key, event);
            log.info("Evento Kafka publicado en {}: type={}, orderId={}, eventId={}",
                    TOPIC_ORDERS_EVENTS, eventType, order.getId(), eventId);

        } catch (Exception e) {
            log.error("Error al publicar evento en Kafka para pedido {}: {}", order.getId(), e.getMessage());
        }
    }
}
