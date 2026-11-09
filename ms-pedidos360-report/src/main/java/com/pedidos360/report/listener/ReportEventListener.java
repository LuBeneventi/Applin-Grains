package com.pedidos360.report.listener;

import com.pedidos360.report.service.ReportService;
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
 * Consumidor Kafka en ms-pedidos360-report que escucha 'orders.events'.
 * Actualiza en tiempo real los datos para SalesChart, LeadTimeChart y TopProductsChart.
 * Cuenta con política de reintentos y DLT (*.DLT: orders.events.DLT).
 */
@Component
public class ReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);

    private final ReportService reportService;

    public ReportEventListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "orders.events", groupId = "pedidos360-report-group")
    public void consumeOrderEvent(String messageJson,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.OFFSET) Long offset) {
        log.info("Mensaje recibido en reportes desde {}: offset={}", topic, offset);
        reportService.processOrderDomainEvent(messageJson);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.OFFSET) Long offset) {
        log.error("Mensaje enviado a DLT {}: offset={}, key={}, value={}",
                topic, offset, record.key(), record.value());
    }
}
