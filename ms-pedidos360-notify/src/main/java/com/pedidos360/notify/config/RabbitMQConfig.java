package com.pedidos360.notify.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de topología RabbitMQ:
 * - Exchanges:
 *     - Direct: cmd.direct
 *     - Topic: cmd.topic
 *     - Dead Letter (Direct): cmd.dead.dlx (compatible con cmd.dlx)
 * - Colas principales con DLQ routing:
 *     - q.cmd.email -> DLX cmd.dead.dlx (routing key: email.send)
 *     - q.cmd.invoice -> DLX cmd.dead.dlx (routing key: invoice.gen)
 * - Colas de descarte DLQ:
 *     - q.cmd.email.dlq
 *     - q.cmd.invoice.dlq
 * - Bindings:
 *     - Direct:
 *         cmd.direct -> q.cmd.email con "email.send" y "cmd.email"
 *         cmd.direct -> q.cmd.invoice con "invoice.gen" y "cmd.invoice"
 *     - Topic:
 *         cmd.topic -> q.cmd.email con "email.*"
 *         cmd.topic -> q.cmd.invoice con "invoice.*"
 *     - Dead Letter:
 *         cmd.dead.dlx -> q.cmd.email.dlq con "email.send"
 *         cmd.dead.dlx -> q.cmd.invoice.dlq con "invoice.gen"
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";
    public static final String EXCHANGE_CMD_DEAD_DLX = "cmd.dead.dlx";
    public static final String EXCHANGE_CMD_DLX = EXCHANGE_CMD_DEAD_DLX;
    public static final String EXCHANGE_CMD_DLX_LEGACY = "cmd.dlx";

    public static final String QUEUE_CMD_EMAIL = "q.cmd.email";
    public static final String QUEUE_CMD_INVOICE = "q.cmd.invoice";
    public static final String QUEUE_CMD_STOCK = "q.cmd.stock";

    public static final String QUEUE_CMD_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String QUEUE_CMD_INVOICE_DLQ = "q.cmd.invoice.dlq";
    public static final String QUEUE_CMD_STOCK_DLQ = "q.cmd.stock.dlq";

    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_INVOICE_GEN = "invoice.gen";
    public static final String ROUTING_KEY_STOCK_REDUCE = "stock.reduce";

    public static final String ROUTING_KEY_EMAIL_TOPIC = "email.*";
    public static final String ROUTING_KEY_INVOICE_TOPIC = "invoice.*";
    public static final String ROUTING_KEY_STOCK_TOPIC = "stock.*";

    // Compatibilidad legacy
    public static final String ROUTING_KEY_EMAIL = "cmd.email";
    public static final String ROUTING_KEY_INVOICE = "cmd.invoice";
    public static final String ROUTING_KEY_STOCK = "cmd.stock";
    public static final String ROUTING_KEY_EMAIL_DLQ = "cmd.email.dlq";
    public static final String ROUTING_KEY_INVOICE_DLQ = "cmd.invoice.dlq";
    public static final String ROUTING_KEY_STOCK_DLQ = "cmd.stock.dlq";

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    @Bean
    public DirectExchange cmdDeadDlxExchange() {
        return new DirectExchange(EXCHANGE_CMD_DEAD_DLX, true, false);
    }

    @Bean
    public DirectExchange cmdDlxExchange() {
        return new DirectExchange(EXCHANGE_CMD_DLX_LEGACY, true, false);
    }

    // Colas DLQ
    @Bean
    public Queue emailDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL_DLQ).build();
    }

    @Bean
    public Queue invoiceDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_INVOICE_DLQ).build();
    }

    @Bean
    public Queue stockDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_STOCK_DLQ).build();
    }

    // Bindings DLQ a cmd.dead.dlx
    @Bean
    public Binding emailDeadDlxBinding(Queue emailDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(emailDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_EMAIL_SEND);
    }

    @Bean
    public Binding invoiceDeadDlxBinding(Queue invoiceDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(invoiceDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_INVOICE_GEN);
    }

    @Bean
    public Binding stockDeadDlxBinding(Queue stockDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(stockDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_STOCK_REDUCE);
    }

    // Bindings de compatibilidad legacy hacia cmd.dlx
    @Bean
    public Binding emailDlqBinding(Queue emailDlqQueue, DirectExchange cmdDlxExchange) {
        return BindingBuilder.bind(emailDlqQueue).to(cmdDlxExchange).with(ROUTING_KEY_EMAIL_DLQ);
    }

    @Bean
    public Binding invoiceDlqBinding(Queue invoiceDlqQueue, DirectExchange cmdDlxExchange) {
        return BindingBuilder.bind(invoiceDlqQueue).to(cmdDlxExchange).with(ROUTING_KEY_INVOICE_DLQ);
    }

    @Bean
    public Binding stockDlqBinding(Queue stockDlqQueue, DirectExchange cmdDlxExchange) {
        return BindingBuilder.bind(stockDlqQueue).to(cmdDlxExchange).with(ROUTING_KEY_STOCK_DLQ);
    }

    // Colas principales con Dead Letter configurado hacia cmd.dead.dlx
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_EMAIL_SEND)
                .build();
    }

    @Bean
    public Queue invoiceQueue() {
        return QueueBuilder.durable(QUEUE_CMD_INVOICE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_INVOICE_GEN)
                .build();
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(QUEUE_CMD_STOCK)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_STOCK_REDUCE)
                .build();
    }

    // Bindings Direct
    @Bean
    public Binding emailDirectBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(ROUTING_KEY_EMAIL_SEND);
    }

    @Bean
    public Binding invoiceDirectBinding(Queue invoiceQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(invoiceQueue).to(cmdDirectExchange).with(ROUTING_KEY_INVOICE_GEN);
    }

    @Bean
    public Binding stockDirectBinding(Queue stockQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(stockQueue).to(cmdDirectExchange).with(ROUTING_KEY_STOCK_REDUCE);
    }

    // Bindings Direct Legacy ("cmd.email", "cmd.invoice", "cmd.stock")
    @Bean
    public Binding emailLegacyDirectBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding invoiceLegacyDirectBinding(Queue invoiceQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(invoiceQueue).to(cmdDirectExchange).with(ROUTING_KEY_INVOICE);
    }

    @Bean
    public Binding stockLegacyDirectBinding(Queue stockQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(stockQueue).to(cmdDirectExchange).with(ROUTING_KEY_STOCK);
    }

    // Bindings Topic
    @Bean
    public Binding emailTopicBinding(Queue emailQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdTopicExchange).with(ROUTING_KEY_EMAIL_TOPIC);
    }

    @Bean
    public Binding invoiceTopicBinding(Queue invoiceQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(invoiceQueue).to(cmdTopicExchange).with(ROUTING_KEY_INVOICE_TOPIC);
    }

    @Bean
    public Binding stockTopicBinding(Queue stockQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(stockQueue).to(cmdTopicExchange).with(ROUTING_KEY_STOCK_TOPIC);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }
}
