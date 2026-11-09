package com.pedidos360.report.dto;

public class LeadTimeMetricsDto {

    private Double avgAcceptMinutes;
    private Double avgPrepareMinutes;
    private Double avgDispatchMinutes;
    private Double avgDeliveryMinutes;
    private Double avgTotalMinutes;

    public LeadTimeMetricsDto() {
    }

    public LeadTimeMetricsDto(Double avgAcceptMinutes, Double avgPrepareMinutes,
                              Double avgDispatchMinutes, Double avgDeliveryMinutes,
                              Double avgTotalMinutes) {
        this.avgAcceptMinutes = avgAcceptMinutes;
        this.avgPrepareMinutes = avgPrepareMinutes;
        this.avgDispatchMinutes = avgDispatchMinutes;
        this.avgDeliveryMinutes = avgDeliveryMinutes;
        this.avgTotalMinutes = avgTotalMinutes;
    }

    public Double getAvgAcceptMinutes() {
        return avgAcceptMinutes;
    }

    public void setAvgAcceptMinutes(Double avgAcceptMinutes) {
        this.avgAcceptMinutes = avgAcceptMinutes;
    }

    public Double getAvgPrepareMinutes() {
        return avgPrepareMinutes;
    }

    public void setAvgPrepareMinutes(Double avgPrepareMinutes) {
        this.avgPrepareMinutes = avgPrepareMinutes;
    }

    public Double getAvgDispatchMinutes() {
        return avgDispatchMinutes;
    }

    public void setAvgDispatchMinutes(Double avgDispatchMinutes) {
        this.avgDispatchMinutes = avgDispatchMinutes;
    }

    public Double getAvgDeliveryMinutes() {
        return avgDeliveryMinutes;
    }

    public void setAvgDeliveryMinutes(Double avgDeliveryMinutes) {
        this.avgDeliveryMinutes = avgDeliveryMinutes;
    }

    public Double getAvgTotalMinutes() {
        return avgTotalMinutes;
    }

    public void setAvgTotalMinutes(Double avgTotalMinutes) {
        this.avgTotalMinutes = avgTotalMinutes;
    }
}
