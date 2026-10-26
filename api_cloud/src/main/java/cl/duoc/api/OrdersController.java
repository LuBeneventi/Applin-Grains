package cl.duoc.api;

import cl.duoc.api.dto.EnrichedOrderItemDto;
import cl.duoc.api.dto.EnrichedOrderResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NOTA: la creación de pedidos (POST /api/orders) ya no está aquí:
 * ahora la maneja CheckoutController (identidad por token, precios del catálogo
 * y pago verificado).
 * La edición/baja de productos (PUT/DELETE) está en AdminCatalogController.
 */
@RestController
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final RestClient ordersServiceClient;
    private final RestClient catalogServiceClient;
    private final ObjectMapper objectMapper;

    public OrdersController(
            @Qualifier("ordersServiceClient") RestClient ordersServiceClient,
            @Qualifier("catalogServiceClient") RestClient catalogServiceClient,
            ObjectMapper objectMapper) {
        this.ordersServiceClient = ordersServiceClient;
        this.catalogServiceClient = catalogServiceClient;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // ENDPOINTS DE CATÁLOGO PROXY EN EL BFF
    // ==========================================

    @GetMapping("/api/catalog/products")
    public String getAllProducts() {
        return catalogServiceClient
                .get()
                .uri("/api/catalog/products")
                .retrieve()
                .body(String.class);
    }

    @GetMapping("/api/catalog/products/{id}")
    public String getProductById(@PathVariable String id) {
        return catalogServiceClient
                .get()
                .uri("/api/catalog/products/" + id)
                .retrieve()
                .body(String.class);
    }

    @PostMapping(value = "/api/catalog/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProduct(@RequestBody String body) {
        return catalogServiceClient
                .post()
                .uri("/api/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping("/api/catalog/products/{id}/reduce-stock")
    public ResponseEntity<String> reduceProductStock(
            @PathVariable String id,
            @RequestParam(name = "quantity", defaultValue = "1") Integer quantity) {
        return catalogServiceClient
                .post()
                .uri("/api/catalog/products/" + id + "/reduce-stock?quantity=" + quantity)
                .retrieve()
                .toEntity(String.class);
    }

    // ==========================================
    // ENDPOINTS DE ÓRDENES EN EL BFF
    // ==========================================

    @GetMapping("/api/orders")
    public String orders(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/orders" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return ordersServiceClient
                .get()
                .uri(targetUri)
                .retrieve()
                .body(String.class);
    }

    @GetMapping("/api/orders/{id}")
    public String getOrderById(@PathVariable String id) {
        return ordersServiceClient
                .get()
                .uri("/api/orders/" + id)
                .retrieve()
                .body(String.class);
    }

    @PutMapping(value = "/api/orders/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateOrderStatus(@PathVariable String id, @RequestBody String body) {
        return ordersServiceClient
                .put()
                .uri("/api/orders/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    // ==========================================
    // ENDPOINT BFF ENRIQUECIDO (AGREGADOR)
    // ==========================================

    /**
     * Endpoint BFF enriquecido: obtiene la orden desde el microservicio de órdenes
     * y para cada ítem que posea productoId consulta el catálogo para resolver
     * nombre, descripción, stock, imagen, etc.
     */
    @GetMapping("/api/orders/{id}/enriched")
    public ResponseEntity<?> getEnrichedOrderById(@PathVariable String id) {
        try {
            String rawOrder = ordersServiceClient
                    .get()
                    .uri("/api/orders/" + id)
                    .retrieve()
                    .body(String.class);

            if (rawOrder == null) {
                return ResponseEntity.notFound().build();
            }

            JsonNode orderNode = objectMapper.readTree(rawOrder);
            Long orderId = orderNode.hasNonNull("id") ? orderNode.get("id").asLong() : null;
            String clienteId = orderNode.hasNonNull("clienteId") ? orderNode.get("clienteId").asText() : null;
            String clienteNombre = orderNode.hasNonNull("clienteNombre") ? orderNode.get("clienteNombre").asText()
                    : null;
            String clienteEmail = orderNode.hasNonNull("clienteEmail") ? orderNode.get("clienteEmail").asText() : null;
            BigDecimal total = orderNode.hasNonNull("total") ? new BigDecimal(orderNode.get("total").asText()) : null;
            String estado = orderNode.hasNonNull("estado") ? orderNode.get("estado").asText() : null;
            String fechaCreacion = orderNode.hasNonNull("fechaCreacion") ? orderNode.get("fechaCreacion").asText()
                    : null;
            String fechaActualizacion = orderNode.hasNonNull("fechaActualizacion")
                    ? orderNode.get("fechaActualizacion").asText()
                    : null;

            List<EnrichedOrderItemDto> enrichedItems = new ArrayList<>();
            if (orderNode.has("items") && orderNode.get("items").isArray()) {
                for (JsonNode itemNode : orderNode.get("items")) {
                    Long itemId = itemNode.hasNonNull("id") ? itemNode.get("id").asLong() : null;
                    Long productoId = itemNode.hasNonNull("productoId") ? itemNode.get("productoId").asLong() : null;
                    String descripcion = itemNode.hasNonNull("descripcion") ? itemNode.get("descripcion").asText()
                            : null;
                    Integer cantidad = itemNode.hasNonNull("cantidad") ? itemNode.get("cantidad").asInt() : null;
                    BigDecimal precioUnitario = itemNode.hasNonNull("precioUnitario")
                            ? new BigDecimal(itemNode.get("precioUnitario").asText())
                            : null;
                    BigDecimal subtotal = itemNode.hasNonNull("subtotal")
                            ? new BigDecimal(itemNode.get("subtotal").asText())
                            : null;

                    Map<String, Object> productDetail = null;
                    if (productoId != null) {
                        try {
                            String productRaw = catalogServiceClient
                                    .get()
                                    .uri("/api/catalog/products/" + productoId)
                                    .retrieve()
                                    .body(String.class);
                            if (productRaw != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = objectMapper.readValue(productRaw, Map.class);
                                productDetail = map;
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo obtener el detalle del producto {} del catálogo: {}", productoId,
                                    e.getMessage());
                        }
                    }

                    enrichedItems.add(new EnrichedOrderItemDto(
                            itemId,
                            productoId,
                            descripcion,
                            cantidad,
                            precioUnitario,
                            subtotal,
                            productDetail));
                }
            }

            EnrichedOrderResponseDto enrichedResponse = new EnrichedOrderResponseDto(
                    orderId,
                    clienteId,
                    clienteNombre,
                    clienteEmail,
                    enrichedItems,
                    total,
                    estado,
                    fechaCreacion,
                    fechaActualizacion);

            return ResponseEntity.ok(enrichedResponse);
        } catch (Exception e) {
            log.error("Error al procesar orden enriquecida con id {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}