package com.pedidos360.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.dto.DailySalesDto;
import com.pedidos360.report.dto.TopProductDto;
import com.pedidos360.report.entity.ReportOrder;
import com.pedidos360.report.repository.ReportOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportOrderRepository reportOrderRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Debe proyectar una orden creada desde evento Kafka")
    void testProcessOrderCreated() {
        String json = """
        {
          "eventType": "OrderCreated",
          "orderId": 100,
          "orderNumber": "PED-100",
          "clienteId": "CLI-1",
          "clienteNombre": "Carlos",
          "currentStatus": "CREADO",
          "total": 5000.00,
          "timestamp": "2026-11-09T10:00:00Z",
          "items": [
            { "productoId": 1, "descripcion": "Espresso", "cantidad": 2, "precioUnitario": 2500.00, "subtotal": 5000.00 }
          ]
        }
        """;

        when(reportOrderRepository.existsById(100L)).thenReturn(false);
        when(reportOrderRepository.save(any(ReportOrder.class))).thenAnswer(i -> i.getArgument(0));

        reportService.processOrderDomainEvent(json);

        verify(reportOrderRepository).save(any(ReportOrder.class));
    }

    @Test
    @DisplayName("Debe actualizar el estado y calcular lead times")
    void testProcessOrderStatusChanged() {
        ReportOrder existing = new ReportOrder(100L, "PED-100", "CLI-1", "Carlos", "CREADO", new BigDecimal("5000.00"), LocalDate.now(), java.time.Instant.parse("2026-11-09T10:00:00Z"));
        when(reportOrderRepository.findById(100L)).thenReturn(Optional.of(existing));

        String json = """
        {
          "eventType": "OrderAccepted",
          "orderId": 100,
          "currentStatus": "ACEPTADO",
          "timestamp": "2026-11-09T10:05:00Z"
        }
        """;

        reportService.processOrderDomainEvent(json);

        assertEquals("ACEPTADO", existing.getCurrentStatus());
        assertNotNull(existing.getAcceptedAt());
        assertEquals(300L, existing.getLeadTimeAcceptSeconds()); // 5 minutos = 300s
        verify(reportOrderRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar métricas formateadas para SalesChart")
    void testGetSalesChartData() {
        LocalDate today = LocalDate.now();
        List<Object[]> queryResult = new ArrayList<>();
        queryResult.add(new Object[]{today, 5L, new BigDecimal("25000.00")});

        when(reportOrderRepository.getSalesBetweenDates(any(), any())).thenReturn(queryResult);

        List<DailySalesDto> data = reportService.getSalesChartData(today.minusDays(7), today);

        assertEquals(1, data.size());
        assertEquals(5L, data.get(0).getOrderCount());
        assertEquals(new BigDecimal("25000.00"), data.get(0).getTotalAmount());
    }

    @Test
    @DisplayName("Debe retornar ranking para TopProductsChart")
    void testGetTopProductsChartData() {
        List<Object[]> queryResult = new ArrayList<>();
        queryResult.add(new Object[]{1L, "Espresso Doble", 20L, new BigDecimal("50000.00")});
        queryResult.add(new Object[]{2L, "Latte Caramelo", 15L, new BigDecimal("45000.00")});

        when(reportOrderRepository.getTopSellingProducts()).thenReturn(queryResult);

        List<TopProductDto> data = reportService.getTopProductsChartData(5);

        assertEquals(2, data.size());
        assertEquals("Espresso Doble", data.get(0).getDescripcion());
        assertEquals(20L, data.get(0).getTotalQuantitySold());
    }
}
