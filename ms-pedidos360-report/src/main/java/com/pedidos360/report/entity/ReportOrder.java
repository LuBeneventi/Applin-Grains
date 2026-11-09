package com.pedidos360.report.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Almacena el pedido proyectado para reportería, ventas y cálculo de lead time.
 */
@Entity
@Table(name = "report_orders", indexes = {
    @Index(name = "idx_report_order_created", columnList = "createdAt"),
    @Index(name = "idx_report_order_status", columnList = "currentStatus")
})
public class ReportOrder {

    @Id
    private Long orderId;

    @Column(nullable = false, length = 64)
    private String orderNumber;

    @Column(length = 64)
    private String clienteId;

    @Column(length = 128)
    private String clienteNombre;

    @Column(nullable = false, length = 64)
    private String currentStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDate saleDate; // Para agrupaciones rápidas en SalesChart

    @Column(nullable = false)
    private Instant createdAt;

    private Instant acceptedAt;
    private Instant preparingAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    // Lead times en segundos calculados en tiempo real
    private Long leadTimeAcceptSeconds; // createdAt -> acceptedAt
    private Long leadTimePrepareSeconds; // acceptedAt -> preparingAt
    private Long leadTimeDispatchSeconds; // preparingAt -> dispatchedAt
    private Long leadTimeDeliverySeconds; // dispatchedAt -> deliveredAt
    private Long totalLeadTimeSeconds; // createdAt -> deliveredAt

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReportOrderItem> items = new ArrayList<>();

    public ReportOrder() {
    }

    public ReportOrder(Long orderId, String orderNumber, String clienteId, String clienteNombre,
                       String currentStatus, BigDecimal total, LocalDate saleDate, Instant createdAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.currentStatus = currentStatus;
        this.total = total;
        this.saleDate = saleDate;
        this.createdAt = createdAt;
    }

    public void addItem(ReportOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getPreparingAt() {
        return preparingAt;
    }

    public void setPreparingAt(Instant preparingAt) {
        this.preparingAt = preparingAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getLeadTimeAcceptSeconds() {
        return leadTimeAcceptSeconds;
    }

    public void setLeadTimeAcceptSeconds(Long leadTimeAcceptSeconds) {
        this.leadTimeAcceptSeconds = leadTimeAcceptSeconds;
    }

    public Long getLeadTimePrepareSeconds() {
        return leadTimePrepareSeconds;
    }

    public void setLeadTimePrepareSeconds(Long leadTimePrepareSeconds) {
        this.leadTimePrepareSeconds = leadTimePrepareSeconds;
    }

    public Long getLeadTimeDispatchSeconds() {
        return leadTimeDispatchSeconds;
    }

    public void setLeadTimeDispatchSeconds(Long leadTimeDispatchSeconds) {
        this.leadTimeDispatchSeconds = leadTimeDispatchSeconds;
    }

    public Long getLeadTimeDeliverySeconds() {
        return leadTimeDeliverySeconds;
    }

    public void setLeadTimeDeliverySeconds(Long leadTimeDeliverySeconds) {
        this.leadTimeDeliverySeconds = leadTimeDeliverySeconds;
    }

    public Long getTotalLeadTimeSeconds() {
        return totalLeadTimeSeconds;
    }

    public void setTotalLeadTimeSeconds(Long totalLeadTimeSeconds) {
        this.totalLeadTimeSeconds = totalLeadTimeSeconds;
    }

    public List<ReportOrderItem> getItems() {
        return items;
    }

    public void setItems(List<ReportOrderItem> items) {
        this.items = items;
    }
}
