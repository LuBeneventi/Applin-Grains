package com.pedidos360.catalog.dto.event;

import java.time.Instant;
import java.util.List;

public class StockReduceEventPayload {

    private Long orderId;
    private String orderNumber;
    private List<StockItemEventDto> items;
    private Instant timestamp;

    public StockReduceEventPayload() {
    }

    public StockReduceEventPayload(Long orderId, String orderNumber, List<StockItemEventDto> items, Instant timestamp) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.items = items;
        this.timestamp = timestamp;
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

    public List<StockItemEventDto> getItems() {
        return items;
    }

    public void setItems(List<StockItemEventDto> items) {
        this.items = items;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
