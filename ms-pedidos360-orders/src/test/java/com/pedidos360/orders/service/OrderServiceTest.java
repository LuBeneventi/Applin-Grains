package com.pedidos360.orders.service;

import com.pedidos360.orders.dto.OrderItemRequestDto;
import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.repository.OrderRepository;
import com.pedidos360.orders.service.exception.OrderNotFoundException;
import com.pedidos360.orders.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private KafkaOrderEventPublisher kafkaOrderEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order("CLI-123");
        sampleOrder.setId(1L);
        sampleOrder.setEstado(OrderStatus.CREADO);
        sampleOrder.setClienteNombre("Carlos Ruiz");
        sampleOrder.setClienteEmail("carlos@example.com");

        OrderItem item = new OrderItem(1L, "Café Americano", 2, new BigDecimal("2500.00"));
        sampleOrder.addItem(item);
    }

    @Test
    @DisplayName("Debe crear un pedido exitosamente y emitir eventos RabbitMQ y Kafka")
    void testCreateOrder() {
        OrderRequestDto request = new OrderRequestDto();
        request.setClienteId("CLI-123");
        request.setClienteNombre("Carlos Ruiz");
        request.setClienteEmail("carlos@example.com");

        OrderItemRequestDto itemRequest = new OrderItemRequestDto();
        itemRequest.setProductoId(1L);
        itemRequest.setDescripcion("Café Americano");
        itemRequest.setCantidad(2);
        itemRequest.setPrecioUnitario(new BigDecimal("2500.00"));
        request.setItems(List.of(itemRequest));

        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponseDto response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.CREADO, response.getEstado());
        verify(orderRepository).save(any(Order.class));
        verify(orderEventPublisher).publishOrderCreatedEvents(any(Order.class));
        verify(kafkaOrderEventPublisher).publishOrderCreated(any(Order.class));
    }

    @Test
    @DisplayName("Debe actualizar el estado y emitir eventos RabbitMQ y Kafka")
    void testUpdateOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(orderStateMachine).validateTransition(OrderStatus.CREADO, OrderStatus.ACEPTADO);
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponseDto response = orderService.updateOrderStatus(1L, OrderStatus.ACEPTADO);

        assertNotNull(response);
        verify(orderStateMachine).validateTransition(OrderStatus.CREADO, OrderStatus.ACEPTADO);
        verify(orderEventPublisher).publishOrderStatusChangedEvent(any(Order.class), eq(OrderStatus.CREADO), eq(OrderStatus.ACEPTADO));
        verify(kafkaOrderEventPublisher).publishOrderStatusChanged(any(Order.class), eq(OrderStatus.CREADO), eq(OrderStatus.ACEPTADO));
    }

    @Test
    @DisplayName("Debe lanzar excepción al consultar pedido inexistente")
    void testGetOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    @DisplayName("Debe listar pedidos por estado")
    void testListOrdersByStatus() {
        when(orderRepository.findByEstado(OrderStatus.CREADO)).thenReturn(List.of(sampleOrder));

        List<OrderResponseDto> list = orderService.listOrders(OrderStatus.CREADO, null, null);

        assertEquals(1, list.size());
        verify(orderRepository).findByEstado(OrderStatus.CREADO);
    }

    @Test
    @DisplayName("Debe listar pedidos con rango de fechas")
    void testListOrdersWithDateRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        when(orderRepository.findByEstadoAndFechaCreacionBetween(OrderStatus.CREADO, from, to))
                .thenReturn(List.of(sampleOrder));

        List<OrderResponseDto> list = orderService.listOrders(OrderStatus.CREADO, from, to);

        assertEquals(1, list.size());
        verify(orderRepository).findByEstadoAndFechaCreacionBetween(OrderStatus.CREADO, from, to);
    }
}
