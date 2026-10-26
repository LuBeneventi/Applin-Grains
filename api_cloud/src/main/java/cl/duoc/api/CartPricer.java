package cl.duoc.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valora un carrito SIEMPRE con datos del catálogo (nombre, precio y stock reales).
 * Lo que envíe el cliente (precios, nombres) se ignora: solo cuentan productoId y cantidad.
 */
@Component
public class CartPricer {

    public record Line(long productoId, String name, BigDecimal price, int quantity) {}

    public record PricedCart(List<Line> lines, BigDecimal total) {
        public Map<Long, Integer> quantities() {
            Map<Long, Integer> m = new HashMap<>();
            lines.forEach(l -> m.put(l.productoId(), l.quantity()));
            return m;
        }
    }

    private final RestClient catalog;
    private final ObjectMapper mapper;

    public CartPricer(@Qualifier("catalogServiceClient") RestClient catalog, ObjectMapper mapper) {
        this.catalog = catalog;
        this.mapper = mapper;
    }

    public PricedCart price(JsonNode items) {
        if (items == null || !items.isArray() || items.isEmpty()) {
            throw new ApiProblem(HttpStatus.BAD_REQUEST, "EMPTY_CART", "El pedido debe incluir al menos un producto.");
        }
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (JsonNode it : items) {
            long id = it.path("productoId").asLong(0);
            int qty = it.path("cantidad").asInt(0);
            if (id <= 0 || qty < 1 || qty > 99) {
                throw new ApiProblem(HttpStatus.BAD_REQUEST, "INVALID_ITEM", "Producto o cantidad no válidos.");
            }
            wanted.merge(id, qty, Integer::sum);
        }

        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            JsonNode p = fetch(e.getKey());
            String name = p.path("name").asText("Producto " + e.getKey());
            int stock = p.path("stock").asInt(0);
            if (stock < e.getValue()) {
                throw new ApiProblem(HttpStatus.CONFLICT, "InvalidStockException",
                        stock == 0 ? "\"" + name + "\" se agotó." : "Solo quedan " + stock + " unidades de \"" + name + "\".");
            }
            BigDecimal price = new BigDecimal(p.path("price").asText("0"));
            lines.add(new Line(e.getKey(), name, price, e.getValue()));
            total = total.add(price.multiply(BigDecimal.valueOf(e.getValue())));
        }
        return new PricedCart(lines, total);
    }

    private JsonNode fetch(long id) {
        try {
            String raw = catalog.get().uri("/api/catalog/products/" + id).retrieve().body(String.class);
            return mapper.readTree(raw);
        } catch (Exception ex) {
            throw new ApiProblem(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_FOUND", "El producto " + id + " no existe o no está disponible.");
        }
    }
}
