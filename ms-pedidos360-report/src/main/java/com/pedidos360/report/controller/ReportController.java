package com.pedidos360.report.controller;

import com.pedidos360.report.dto.DailySalesDto;
import com.pedidos360.report.dto.KpiSummaryDto;
import com.pedidos360.report.dto.LeadTimeMetricsDto;
import com.pedidos360.report.dto.TopProductDto;
import com.pedidos360.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints del microservicio de reportería.
 * Alimenta los subcomponentes del frontend:
 * - SalesChartComponent
 * - LeadTimeChartComponent
 * - TopProductsChartComponent
 * - ReportsComponent (resumen consolidado)
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports & KPIs", description = "Endpoints de Reportería, KPIs y Gráficos en Tiempo Real")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Obtener resumen de KPIs en tiempo real",
               description = "KPIs globales: total pedidos, ventas, ticket promedio, lead time promedio y top productos.")
    public ResponseEntity<KpiSummaryDto> getKpiSummary() {
        return ResponseEntity.ok(reportService.getKpiSummary());
    }

    @GetMapping("/sales-chart")
    @Operation(summary = "Datos para SalesChartComponent",
               description = "Evolución de ventas agrupadas por fecha en un rango determinado.")
    public ResponseEntity<List<DailySalesDto>> getSalesChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.getSalesChartData(from, to));
    }

    @GetMapping("/lead-time-chart")
    @Operation(summary = "Datos para LeadTimeChartComponent",
               description = "Promedios de tiempo de ciclo por fase (aceptación, preparación, despacho, entrega y total).")
    public ResponseEntity<LeadTimeMetricsDto> getLeadTimeChart() {
        return ResponseEntity.ok(reportService.getLeadTimeChartData());
    }

    @GetMapping("/top-products-chart")
    @Operation(summary = "Datos para TopProductsChartComponent",
               description = "Ranking de productos más vendidos por unidades y recaudación.")
    public ResponseEntity<List<TopProductDto>> getTopProductsChart(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(reportService.getTopProductsChartData(limit));
    }
}
