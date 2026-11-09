package com.pedidos360.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySalesDto {

    private LocalDate date;
    private Long orderCount;
    private BigDecimal totalAmount;

    public DailySalesDto() {
    }

    public DailySalesDto(LocalDate date, Long orderCount, BigDecimal totalAmount) {
        this.date = date;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
