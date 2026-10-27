package com.pedidos360.orders.service;

import com.pedidos360.orders.config.RabbitMQConfig;
import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publicador de eventos hacia RabbitMQ:
 * 1. cmd.email.send -> q.cmd.email (notificaciones al cliente)
 * 2. cmd.invoice.generate -> q.cmd.invoice (facturación y boleta)
 * 3. cmd.stock.reduce -> q.cmd.stock (descuento de inventario en catálogo)
 */
@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Emite los eventos de creación de pedido hacia RabbitMQ:
     * 1. Notificación por correo a la cola q.cmd.email
     * 2. Generación de boleta tributaria a la cola q.cmd.invoice
     * 3. Descuento de stock en inventario a la cola q.cmd.stock
     */
    public void publishOrderCreatedEvents(Order order) {
        if (order == null) {
            return;
        }

        try {
            publishEmailConfirmation(order);
        } catch (Exception e) {
            log.error("Error al publicar evento de correo de creación para pedido {}: {}", order.getId(), e.getMessage());
        }

        try {
            publishInvoiceGeneration(order);
        } catch (Exception e) {
            log.error("Error al publicar evento de facturación para pedido {}: {}", order.getId(), e.getMessage());
        }

        try {
            publishStockReduce(order);
        } catch (Exception e) {
            log.error("Error al publicar evento de descuento de stock para pedido {}: {}", order.getId(), e.getMessage());
        }
    }

    /**
     * Emite evento de cambio de estado a la cola q.cmd.email.
     */
    public void publishOrderStatusChangedEvent(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        if (order == null || newStatus == null) {
            return;
        }

        try {
            String eventId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString().replace("-", "");
            String correlationId = "corr-order-" + order.getId();

            String subject;
            String message;
            if (newStatus == OrderStatus.DESPACHADO) {
                subject = "¡Tu pedido #PED-" + order.getId() + " ha sido despachado! 🚚";
                message = "¡Buenas noticias " + order.getClienteNombre() + "! Tu pedido #PED-" + order.getId() +
                        " ha sido DESPACHADO y va en camino a: " +
                        (order.getClienteDireccion() != null ? order.getClienteDireccion() : "tu dirección registrada") + ".";
            } else {
                subject = "Actualización de tu pedido #" + order.getId() + ": " + newStatus.name();
                message = "¡Hola " + order.getClienteNombre() + "! El estado de tu pedido #PED-" + order.getId() +
                        " ha cambiado de " + (previousStatus != null ? previousStatus.name() : "N/A") +
                        " a " + newStatus.name() + ".";
            }

            EmailPayload payload = new EmailPayload(
                    order.getClienteEmail(),
                    subject,
                    "PED-" + order.getId(),
                    order.getClienteNombre(),
                    newStatus.name(),
                    message,
                    Map.of("previousStatus", previousStatus != null ? previousStatus.name() : "",
                           "newStatus", newStatus.name())
            );

            BaseEventEnvelope<EmailPayload> envelope = new BaseEventEnvelope<>(
                    "cmd.email.send",
                    eventId,
                    Instant.now(),
                    traceId,
                    correlationId,
                    payload
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CMD_DIRECT, RabbitMQConfig.ROUTING_KEY_EMAIL_SEND, envelope);
            log.info("Evento de cambio de estado publicado exitosamente a {}: orderId={}, eventId={}",
                    RabbitMQConfig.ROUTING_KEY_EMAIL_SEND, order.getId(), eventId);

        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de estado para pedido {}: {}", order.getId(), e.getMessage());
        }
    }

    private void publishEmailConfirmation(Order order) {
        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String correlationId = "corr-order-" + order.getId();

        EmailPayload payload = new EmailPayload(
                order.getClienteEmail(),
                "Tu pedido #PED-" + order.getId() + " ha sido confirmado - Pedidos360",
                "PED-" + order.getId(),
                order.getClienteNombre(),
                order.getEstado().name(),
                "¡Hola " + order.getClienteNombre() + "! Tu pedido #PED-" + order.getId() +
                        " por un total de $" + order.getTotal() + " ha sido recibido y se encuentra en proceso.",
                Map.of("total", order.getTotal().toString())
        );

        BaseEventEnvelope<EmailPayload> envelope = new BaseEventEnvelope<>(
                "cmd.email.send",
                eventId,
                Instant.now(),
                traceId,
                correlationId,
                payload
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CMD_DIRECT, RabbitMQConfig.ROUTING_KEY_EMAIL_SEND, envelope);
        log.info("Evento de confirmación de pedido publicado exitosamente a {}: orderId={}, eventId={}",
                RabbitMQConfig.ROUTING_KEY_EMAIL_SEND, order.getId(), eventId);
    }

    private void publishInvoiceGeneration(Order order) {
        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String correlationId = "corr-order-" + order.getId();

        List<InvoiceItem> invoiceItems = order.getItems().stream()
                .map(item -> new InvoiceItem(
                        item.getDescripcion(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                ))
                .toList();

        BigDecimal total = order.getTotal();
        BigDecimal subtotal = total.divide(BigDecimal.valueOf(1.19), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);

        CustomerInfo customer = new CustomerInfo(
                order.getClienteNombre(),
                order.getClienteRut() != null ? order.getClienteRut() : "11.111.111-1",
                order.getClienteEmail(),
                order.getClienteDireccion() != null ? order.getClienteDireccion() : "Dirección no informada"
        );

        InvoicePayload payload = new InvoicePayload(
                "PED-" + order.getId(),
                "BOL-2026-" + String.format("%05d", order.getId()),
                "BOLETA",
                Instant.now(),
                customer,
                invoiceItems,
                "CLP",
                subtotal,
                tax,
                total
        );

        BaseEventEnvelope<InvoicePayload> envelope = new BaseEventEnvelope<>(
                "cmd.invoice.generate",
                eventId,
                Instant.now(),
                traceId,
                correlationId,
                payload
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CMD_DIRECT, RabbitMQConfig.ROUTING_KEY_INVOICE_GEN, envelope);
        log.info("Evento de generación de boleta publicado exitosamente a {}: orderId={}, eventId={}",
                RabbitMQConfig.ROUTING_KEY_INVOICE_GEN, order.getId(), eventId);
    }

    private void publishStockReduce(Order order) {
        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String correlationId = "corr-order-" + order.getId();

        List<StockItem> stockItems = order.getItems().stream()
                .filter(item -> item.getProductoId() != null)
                .map(item -> new StockItem(item.getProductoId(), item.getCantidad()))
                .toList();

        if (stockItems.isEmpty()) {
            return;
        }

        StockReducePayload payload = new StockReducePayload(
                order.getId(),
                "PED-" + order.getId(),
                stockItems,
                Instant.now()
        );

        BaseEventEnvelope<StockReducePayload> envelope = new BaseEventEnvelope<>(
                "cmd.stock.reduce",
                eventId,
                Instant.now(),
                traceId,
                correlationId,
                payload
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CMD_DIRECT, RabbitMQConfig.ROUTING_KEY_STOCK_REDUCE, envelope);
        log.info("Evento de descuento de stock publicado exitosamente a {}: orderId={}, eventId={}, items={}",
                RabbitMQConfig.ROUTING_KEY_STOCK_REDUCE, order.getId(), eventId, stockItems.size());
    }
}
