package com.pedidos360.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de métricas consolidado para el componente ReportsComponent del frontend.
 */
public class KpiSummaryDto {

    private Long totalOrders;
    private BigDecimal totalSales;
    private Double averageTicket;
    private LeadTimeMetricsDto leadTime;
    private List<DailySalesDto> salesTrend;
    private List<TopProductDto> topProducts;

    public KpiSummaryDto() {
    }

    public KpiSummaryDto(Long totalOrders, BigDecimal totalSales, Double averageTicket,
                         LeadTimeMetricsDto leadTime, List<DailySalesDto> salesTrend,
                         List<TopProductDto> topProducts) {
        this.totalOrders = totalOrders;
        this.totalSales = totalSales;
        this.averageTicket = averageTicket;
        this.leadTime = leadTime;
        this.salesTrend = salesTrend;
        this.topProducts = topProducts;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Double getAverageTicket() {
        return averageTicket;
    }

    public void setAverageTicket(Double averageTicket) {
        this.averageTicket = averageTicket;
    }

    public LeadTimeMetricsDto getLeadTime() {
        return leadTime;
    }

    public void setLeadTime(LeadTimeMetricsDto leadTime) {
        this.leadTime = leadTime;
    }

    public List<DailySalesDto> getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(List<DailySalesDto> salesTrend) {
        this.salesTrend = salesTrend;
    }

    public List<TopProductDto> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<TopProductDto> topProducts) {
        this.topProducts = topProducts;
    }
}
