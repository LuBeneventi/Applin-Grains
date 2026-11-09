package cl.duoc.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Endpoints del BFF para el módulo de Reportería y KPIs (Kafka streaming).
 * Ruta frontend: /reports (Component: ReportsComponent, Roles: Admin)
 * Subcomponentes:
 * - SalesChartComponent (/api/reports/sales-chart)
 * - LeadTimeChartComponent (/api/reports/lead-time-chart)
 * - TopProductsChartComponent (/api/reports/top-products-chart)
 */
@RestController
public class ReportController {

    private final RestClient reportServiceClient;

    public ReportController(@Qualifier("reportServiceClient") RestClient reportServiceClient) {
        this.reportServiceClient = reportServiceClient;
    }

    @GetMapping("/api/reports/summary")
    public ResponseEntity<String> getSummary() {
        return reportServiceClient
                .get()
                .uri("/api/reports/summary")
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/api/reports/sales-chart")
    public ResponseEntity<String> getSalesChart(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/reports/sales-chart" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return reportServiceClient
                .get()
                .uri(targetUri)
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/api/reports/lead-time-chart")
    public ResponseEntity<String> getLeadTimeChart() {
        return reportServiceClient
                .get()
                .uri("/api/reports/lead-time-chart")
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/api/reports/top-products-chart")
    public ResponseEntity<String> getTopProductsChart(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/reports/top-products-chart" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return reportServiceClient
                .get()
                .uri(targetUri)
                .retrieve()
                .toEntity(String.class);
    }
}
