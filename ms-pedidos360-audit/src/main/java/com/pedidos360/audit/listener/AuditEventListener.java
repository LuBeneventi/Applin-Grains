package com.pedidos360.audit.listener;

import com.pedidos360.audit.service.AuditService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del flujo canónico 'orders.events'.
 * Cuenta con política de reintentos y DLT (*.DLT: orders.events.DLT)
 * con estrategia DltStrategy.FAIL_ON_ERROR o similar según evaluación.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "orders.events", groupId = "pedidos360-audit-group")
    public void consumeOrderEvent(String messageJson,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.OFFSET) Long offset) {
        log.info("Mensaje recibido en audit desde {}: offset={}", topic, offset);
        auditService.processOrderEvent(messageJson);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.OFFSET) Long offset) {
        log.error("Mensaje enviado a DLT {}: offset={}, key={}, value={}",
                topic, offset, record.key(), record.value());
    }
}
