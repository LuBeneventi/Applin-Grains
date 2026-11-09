package com.pedidos360.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.report.dto.*;
import com.pedidos360.report.entity.ReportOrder;
import com.pedidos360.report.entity.ReportOrderItem;
import com.pedidos360.report.repository.ReportOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportOrderRepository reportOrderRepository;
    private final ObjectMapper objectMapper;

    public ReportService(ReportOrderRepository reportOrderRepository, ObjectMapper objectMapper) {
        this.reportOrderRepository = reportOrderRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Procesa los eventos canónicos de Kafka (orders.events) para mantener las proyecciones y KPIs.
     */
    public void processOrderDomainEvent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String eventType = root.path("eventType").asText("");
            Long orderId = root.path("orderId").asLong(0);

            if (orderId <= 0) {
                log.warn("Evento de orden sin ID válido: {}", rawJson);
                return;
            }

            Instant eventTime = root.hasNonNull("timestamp")
                    ? Instant.parse(root.get("timestamp").asText())
                    : Instant.now();

            if ("OrderCreated".equals(eventType)) {
                handleOrderCreated(root, orderId, eventTime);
            } else {
                handleStatusChanged(eventType, orderId, eventTime);
            }

        } catch (Exception e) {
            log.error("Error al procesar evento de Kafka en reportes: {}", e.getMessage(), e);
            throw new RuntimeException("Error en proyección de reportes", e);
        }
    }

    private void handleOrderCreated(JsonNode root, Long orderId, Instant eventTime) {
        if (reportOrderRepository.existsById(orderId)) {
            log.info("Orden {} ya proyectada previamente. Omitiendo duplicado.", orderId);
            return;
        }

        String orderNumber = root.path("orderNumber").asText("PED-" + orderId);
        String clienteId = root.path("clienteId").asText("");
        String clienteNombre = root.path("clienteNombre").asText("");
        String currentStatus = root.path("currentStatus").asText("CREADO");
        BigDecimal total = root.hasNonNull("total") ? new BigDecimal(root.get("total").asText()) : BigDecimal.ZERO;
        LocalDate saleDate = eventTime.atZone(ZoneOffset.UTC).toLocalDate();

        ReportOrder order = new ReportOrder(orderId, orderNumber, clienteId, clienteNombre, currentStatus, total, saleDate, eventTime);

        JsonNode itemsNode = root.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode it : itemsNode) {
                // Soporte para ambos formatos: eventos reales (productId/productName) y legado (productoId/descripcion)
                Long prodId = it.hasNonNull("productId") ? it.path("productId").asLong(0) : it.path("productoId").asLong(0);
                String desc = it.hasNonNull("productName") ? it.path("productName").asText("Producto " + prodId)
                                                           : it.path("descripcion").asText("Producto " + prodId);
                int qty = it.hasNonNull("quantity") ? it.path("quantity").asInt(1) : it.path("cantidad").asInt(1);
                BigDecimal unitPrice = it.hasNonNull("unitPrice") ? new BigDecimal(it.get("unitPrice").asText())
                        : it.hasNonNull("precioUnitario") ? new BigDecimal(it.get("precioUnitario").asText()) : BigDecimal.ZERO;
                BigDecimal subtotal = it.hasNonNull("subtotal") ? new BigDecimal(it.get("subtotal").asText()) : unitPrice.multiply(BigDecimal.valueOf(qty));

                ReportOrderItem item = new ReportOrderItem(prodId, desc, qty, unitPrice, subtotal);
                order.addItem(item);
            }
        }

        reportOrderRepository.save(order);
        log.info("Orden {} proyectada exitosamente en reportes. Total: ${}", orderNumber, total);
    }

    private void handleStatusChanged(String eventType, Long orderId, Instant eventTime) {
        reportOrderRepository.findById(orderId).ifPresent(order -> {
            switch (eventType) {
                case "OrderAccepted" -> {
                    order.setCurrentStatus("ACEPTADO");
                    order.setAcceptedAt(eventTime);
                    if (order.getCreatedAt() != null) {
                        order.setLeadTimeAcceptSeconds(Duration.between(order.getCreatedAt(), eventTime).getSeconds());
                    }
                }
                case "OrderPreparing" -> {
                    order.setCurrentStatus("EN_PREPARACION");
                    order.setPreparingAt(eventTime);
                    Instant base = order.getAcceptedAt() != null ? order.getAcceptedAt() : order.getCreatedAt();
                    if (base != null) {
                        order.setLeadTimePrepareSeconds(Duration.between(base, eventTime).getSeconds());
                    }
                }
                case "OrderDispatched" -> {
                    order.setCurrentStatus("DESPACHADO");
                    order.setDispatchedAt(eventTime);
                    Instant base = order.getPreparingAt() != null ? order.getPreparingAt() : order.getAcceptedAt();
                    if (base != null) {
                        order.setLeadTimeDispatchSeconds(Duration.between(base, eventTime).getSeconds());
                    }
                }
                case "OrderDelivered" -> {
                    order.setCurrentStatus("ENTREGADO");
                    order.setDeliveredAt(eventTime);
                    if (order.getDispatchedAt() != null) {
                        order.setLeadTimeDeliverySeconds(Duration.between(order.getDispatchedAt(), eventTime).getSeconds());
                    }
                    if (order.getCreatedAt() != null) {
                        order.setTotalLeadTimeSeconds(Duration.between(order.getCreatedAt(), eventTime).getSeconds());
                    }
                }
                case "OrderCancelled" -> {
                    order.setCurrentStatus("CANCELADO");
                    order.setCancelledAt(eventTime);
                }
            }
            reportOrderRepository.save(order);
            log.info("Estado de orden {} actualizado a {} en reportes", orderId, order.getCurrentStatus());
        });
    }

    @Transactional(readOnly = true)
    public List<DailySalesDto> getSalesChartData(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Object[]> rawList = reportOrderRepository.getSalesBetweenDates(start, end);
        List<DailySalesDto> result = new ArrayList<>();
        for (Object[] row : rawList) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            BigDecimal sum = (BigDecimal) row[2];
            result.add(new DailySalesDto(date, count, sum));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public LeadTimeMetricsDto getLeadTimeChartData() {
        List<Object[]> rawList = reportOrderRepository.getAverageLeadTimes();
        if (rawList.isEmpty() || rawList.get(0)[0] == null) {
            return new LeadTimeMetricsDto(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        Object[] row = rawList.get(0);
        Double avgAcceptSec = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
        Double avgPrepareSec = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double avgDispatchSec = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Double avgDeliverySec = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double avgTotalSec = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        return new LeadTimeMetricsDto(
                roundToMinutes(avgAcceptSec),
                roundToMinutes(avgPrepareSec),
                roundToMinutes(avgDispatchSec),
                roundToMinutes(avgDeliverySec),
                roundToMinutes(avgTotalSec)
        );
    }

    @Transactional(readOnly = true)
    public List<TopProductDto> getTopProductsChartData(int limit) {
        List<Object[]> rawList = reportOrderRepository.getTopSellingProducts();
        List<TopProductDto> result = new ArrayList<>();
        int count = 0;
        for (Object[] row : rawList) {
            if (count++ >= limit) break;
            Long prodId = (Long) row[0];
            String desc = (String) row[1];
            Long qty = ((Number) row[2]).longValue();
            BigDecimal rev = (BigDecimal) row[3];
            result.add(new TopProductDto(prodId, desc, qty, rev));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public KpiSummaryDto getKpiSummary() {
        List<ReportOrder> all = reportOrderRepository.findAll();
        long totalOrders = all.stream().filter(o -> !"CANCELADO".equals(o.getCurrentStatus())).count();
        BigDecimal totalSales = all.stream()
                .filter(o -> !"CANCELADO".equals(o.getCurrentStatus()))
                .map(ReportOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgTicket = totalOrders > 0
                ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        LeadTimeMetricsDto leadTime = getLeadTimeChartData();
        List<DailySalesDto> salesTrend = getSalesChartData(LocalDate.now().minusDays(14), LocalDate.now());
        List<TopProductDto> topProducts = getTopProductsChartData(5);

        return new KpiSummaryDto(totalOrders, totalSales, avgTicket, leadTime, salesTrend, topProducts);
    }

    private double roundToMinutes(double seconds) {
        return Math.round((seconds / 60.0) * 100.0) / 100.0;
    }
}
