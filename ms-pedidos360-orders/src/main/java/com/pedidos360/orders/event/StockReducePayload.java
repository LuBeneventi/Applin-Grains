package com.pedidos360.orders.event;

import java.time.Instant;
import java.util.List;

public class StockReducePayload {

    private Long orderId;
    private String orderNumber;
    private List<StockItem> items;
    private Instant timestamp;

    public StockReducePayload() {
    }

    public StockReducePayload(Long orderId, String orderNumber, List<StockItem> items, Instant timestamp) {
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

    public List<StockItem> getItems() {
        return items;
    }

    public void setItems(List<StockItem> items) {
        this.items = items;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
