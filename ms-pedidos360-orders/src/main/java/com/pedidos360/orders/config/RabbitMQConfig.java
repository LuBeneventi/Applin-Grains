package com.pedidos360.orders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";

    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_INVOICE_GEN = "invoice.gen";

    // Aliases para compatibilidad
    public static final String ROUTING_KEY_EMAIL = ROUTING_KEY_EMAIL_SEND;
    public static final String ROUTING_KEY_INVOICE = ROUTING_KEY_INVOICE_GEN;

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        mapper.registerModule(new JavaTimeModule());
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        // Desactivar el encabezado __TypeId__ con el nombre del paquete para evitar acoplamiento de clases entre microservicios
        converter.setClassMapper(new org.springframework.amqp.support.converter.DefaultClassMapper() {
            @Override
            public void fromClass(Class<?> clazz, org.springframework.amqp.core.MessageProperties properties) {
                // No inyectar __TypeId__
            }
        });
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
