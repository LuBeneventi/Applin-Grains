package com.pedidos360.orders.service.impl;

import com.pedidos360.orders.dto.OrderItemRequestDto;
import com.pedidos360.orders.dto.OrderItemResponseDto;
import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.repository.OrderRepository;
import com.pedidos360.orders.service.KafkaOrderEventPublisher;
import com.pedidos360.orders.service.OrderEventPublisher;
import com.pedidos360.orders.service.OrderService;
import com.pedidos360.orders.service.OrderStateMachine;
import com.pedidos360.orders.service.exception.OrderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final OrderEventPublisher orderEventPublisher;
    private final KafkaOrderEventPublisher kafkaOrderEventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderStateMachine orderStateMachine,
                            OrderEventPublisher orderEventPublisher,
                            KafkaOrderEventPublisher kafkaOrderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
        this.orderEventPublisher = orderEventPublisher;
        this.kafkaOrderEventPublisher = kafkaOrderEventPublisher;
    }

    @Override
    public OrderResponseDto createOrder(OrderRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de pedido no puede ser nula.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe contener al menos un ítem.");
        }

        Order order = new Order(request.getClienteId());
        order.setEstado(OrderStatus.CREADO);

        if (request.getClienteNombre() != null) {
            order.setClienteNombre(request.getClienteNombre());
        }
        if (request.getClienteEmail() != null) {
            order.setClienteEmail(request.getClienteEmail());
        }
        if (request.getClienteDireccion() != null) {
            order.setClienteDireccion(request.getClienteDireccion());
        }
        if (request.getClienteRut() != null) {
            order.setClienteRut(request.getClienteRut());
        }

        for (OrderItemRequestDto itemDto : request.getItems()) {
            OrderItem item = new OrderItem(
                    itemDto.getProductoId(),
                    itemDto.getDescripcion(),
                    itemDto.getCantidad(),
                    itemDto.getPrecioUnitario()
            );
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);

        // 1. Notificar comandos directos vía RabbitMQ (Email, Invoice, Stock)
        orderEventPublisher.publishOrderCreatedEvents(saved);

        // 2. Notificar evento canónico de negocio vía Apache Kafka (orders.events -> OrderCreated)
        kafkaOrderEventPublisher.publishOrderCreated(saved);

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del pedido no puede ser nulo.");
        }
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return mapToResponseDto(order);
    }

    @Override
    public OrderResponseDto updateOrderStatus(Long id, OrderStatus nuevoEstado) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del pedido no puede ser nulo.");
        }
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Validar la transición con la máquina de estados (lanza InvalidStateTransitionException si no es válida)
        orderStateMachine.validateTransition(order.getEstado(), nuevoEstado);

        OrderStatus previousStatus = order.getEstado();
        order.setEstado(nuevoEstado);
        Order updated = orderRepository.save(order);

        // 1. Notificar cambio de estado vía RabbitMQ (Email)
        orderEventPublisher.publishOrderStatusChangedEvent(updated, previousStatus, nuevoEstado);

        // 2. Notificar cambio de estado canónico vía Kafka (orders.events -> OrderAccepted/OrderPreparing/etc)
        kafkaOrderEventPublisher.publishOrderStatusChanged(updated, previousStatus, nuevoEstado);

        return mapToResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial ('from') no puede ser posterior a la fecha final ('to').");
        }

        List<Order> orders;

        if (status != null && from != null && to != null) {
            orders = orderRepository.findByEstadoAndFechaCreacionBetween(status, from, to);
        } else if (status != null) {
            orders = orderRepository.findByEstado(status);
        } else if (from != null && to != null) {
            orders = orderRepository.findByFechaCreacionBetween(from, to);
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getId(),
                        item.getProductoId(),
                        item.getDescripcion(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getId(),
                order.getClienteId(),
                order.getClienteNombre(),
                order.getClienteEmail(),
                itemDtos,
                order.getTotal(),
                order.getEstado(),
                order.getFechaCreacion(),
                order.getFechaActualizacion()
        );
    }
}
