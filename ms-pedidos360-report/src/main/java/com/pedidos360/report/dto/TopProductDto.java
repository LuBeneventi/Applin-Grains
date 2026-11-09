package com.pedidos360.report.dto;

import java.math.BigDecimal;

public class TopProductDto {

    private Long productoId;
    private String descripcion;
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;

    public TopProductDto() {
    }

    public TopProductDto(Long productoId, String descripcion, Long totalQuantitySold, BigDecimal totalRevenue) {
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.totalQuantitySold = totalQuantitySold;
        this.totalRevenue = totalRevenue;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public void setTotalQuantitySold(Long totalQuantitySold) {
        this.totalQuantitySold = totalQuantitySold;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
