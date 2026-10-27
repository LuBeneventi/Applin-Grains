package com.pedidos360.catalog.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.catalog.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de inventario en RabbitMQ (cola: q.cmd.stock).
 * Descuenta automáticamente el stock de los productos incluidos en las órdenes creadas.
 */
@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public StockEventListener(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "q.cmd.stock")
    public void handleStockReduceEvent(String messageJson) {
        log.info("Mensaje recibido en cola q.cmd.stock: {}", messageJson);

        try {
            JsonNode root = objectMapper.readTree(messageJson);

            // Soporta tanto BaseEventEnvelope (donde viene bajo 'payload') como payload plano
            JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;

            String orderNumber = payloadNode.hasNonNull("orderNumber") ? payloadNode.get("orderNumber").asText() : "N/A";
            JsonNode itemsNode = payloadNode.get("items");

            if (itemsNode == null || !itemsNode.isArray() || itemsNode.isEmpty()) {
                log.warn("El evento de stock para orden {} no contiene ítems a descontar.", orderNumber);
                return;
            }

            for (JsonNode itemNode : itemsNode) {
                Long productoId = itemNode.hasNonNull("productoId") ? itemNode.get("productoId").asLong() : null;
                int cantidad = itemNode.hasNonNull("cantidad") ? itemNode.get("cantidad").asInt() : 1;

                if (productoId != null && cantidad > 0) {
                    try {
                        productService.reduceStock(productoId, cantidad);
                        log.info("Stock descontado exitosamente vía RabbitMQ: orden={}, productoId={}, cantidad={}",
                                orderNumber, productoId, cantidad);
                    } catch (Exception e) {
                        log.error("Fallo al descontar stock para productoId={} en orden {}: {}",
                                productoId, orderNumber, e.getMessage());
                        // En caso de fallo crítico en inventario, relanzamos para activar DLQ
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error al procesar mensaje en q.cmd.stock: {}", e.getMessage(), e);
            throw new RuntimeException("Error en procesamiento de evento de stock", e);
        }
    }
}
