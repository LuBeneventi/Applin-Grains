package com.pedidos360.catalog.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.catalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventListenerTest {

    @Mock
    private ProductService productService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StockEventListener stockEventListener;

    @Test
    @DisplayName("Debe descontar stock para cada ítem del evento correctamente")
    void shouldReduceStockForItemsInEvent() {
        String json = """
        {
          "type": "cmd.stock.reduce",
          "eventId": "evt-1234",
          "payload": {
            "orderId": 1024,
            "orderNumber": "PED-1024",
            "items": [
              { "productoId": 1, "cantidad": 2 },
              { "productoId": 3, "cantidad": 1 }
            ]
          }
        }
        """;

        stockEventListener.handleStockReduceEvent(json);

        verify(productService, times(1)).reduceStock(1L, 2);
        verify(productService, times(1)).reduceStock(3L, 1);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el servicio falla para que RabbitMQ lo envíe a la DLQ")
    void shouldThrowExceptionWhenServiceFails() {
        String json = """
        {
          "payload": {
            "orderNumber": "PED-999",
            "items": [
              { "productoId": 5, "cantidad": 10 }
            ]
          }
        }
        """;

        doThrow(new RuntimeException("Stock insuficiente")).when(productService).reduceStock(5L, 10);

        assertThrows(RuntimeException.class, () -> stockEventListener.handleStockReduceEvent(json));
    }
}
