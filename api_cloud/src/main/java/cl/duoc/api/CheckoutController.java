package cl.duoc.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Creación de pedidos con identidad del token, precios del catálogo y pago
 * verificado,
 * más "mis pedidos". REEMPLAZA al método createOrder de OrdersController
 * (bórralo de allí).
 */
@RestController
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final RestClient ordersServiceClient;
    private final RestClient catalogServiceClient;
    private final ObjectMapper objectMapper;
    private final CartPricer pricer;
    private final PaymentStore payments;
    private final boolean paymentRequired;

    public CheckoutController(
            @Qualifier("ordersServiceClient") RestClient ordersServiceClient,
            @Qualifier("catalogServiceClient") RestClient catalogServiceClient,
            ObjectMapper objectMapper,
            CartPricer pricer,
            PaymentStore payments,
            @Value("${app.payments.required:true}") boolean paymentRequired) {
        this.ordersServiceClient = ordersServiceClient;
        this.catalogServiceClient = catalogServiceClient;
        this.objectMapper = objectMapper;
        this.pricer = pricer;
        this.payments = payments;
        this.paymentRequired = paymentRequired;
    }

    @PostMapping(value = "/api/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createOrder(@RequestBody String body, @AuthenticationPrincipal Jwt jwt)
            throws Exception {
        ObjectNode order = (ObjectNode) objectMapper.readTree(body);

        // 1) Nombres, precios y stock desde el catálogo: se ignora lo que mande el
        // cliente
        CartPricer.PricedCart cart = pricer.price(order.get("items"));

        // 2) Correo de la boleta: el que escribió el cliente si es válido; si no, el de
        // su cuenta.
        // La identidad del dueño del pedido es el "sub" del token (no el correo), así
        // "Mis pedidos"
        // funciona aunque el correo de notificación sea distinto al de la cuenta
        // Microsoft.
        String formEmail = order.path("clienteEmail").asText("").trim();
        if (formEmail.matches("^[^@\\s#]+@[^@\\s#]+\\.[^@\\s#]+$")) {
            order.put("clienteEmail", formEmail);
        } else {
            String tokenEmail = emailOf(jwt);
            if (tokenEmail != null) {
                order.put("clienteEmail", tokenEmail);
            }
        }
        if (jwt != null && jwt.getSubject() != null) {
            order.put("clienteId", ownerId(jwt));
        }

        // 3) El pago debe existir, ser de este usuario y coincidir con el monto y los
        // productos
        String paymentId = order.hasNonNull("paymentId") ? order.get("paymentId").asText() : null;
        order.remove("paymentId");
        if (paymentRequired) {
            payments.consume(paymentId, PaymentStore.ownerOf(jwt), cart);
        }

        ArrayNode items = order.putArray("items");
        for (CartPricer.Line l : cart.lines()) {
            ObjectNode item = items.addObject();
            item.put("productoId", l.productoId());
            item.put("cantidad", l.quantity());
            item.put("descripcion", l.name());
            item.put("precioUnitario", l.price());
        }

        // 4) Crear en ms-orders; si falla, el pago se libera (no se cobró)
        ResponseEntity<String> response;
        try {
            response = ordersServiceClient.post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(order))
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientException e) {
            if (paymentRequired) {
                payments.release(paymentId);
            }
            log.error("ms-orders rechazó la creación del pedido: {}", e.getMessage());
            throw new ApiProblem(HttpStatus.BAD_GATEWAY, "ORDERS_ERROR",
                    "No pudimos crear el pedido. No se realizó ningún cobro.");
        }

        // 5) Descontar stock
        if (response.getStatusCode().is2xxSuccessful()) {
            for (CartPricer.Line l : cart.lines()) {
                try {
                    catalogServiceClient.post()
                            .uri("/api/catalog/products/" + l.productoId() + "/reduce-stock?quantity=" + l.quantity())
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception e) {
                    log.warn("No se pudo reducir el stock de productoId={}: {}", l.productoId(), e.getMessage());
                }
            }
        }
        return response;
    }

    /** Pedidos del usuario autenticado (filtra por el correo del token). */
    @GetMapping("/api/orders/mine")
    public ResponseEntity<ArrayNode> myOrders(@AuthenticationPrincipal Jwt jwt) throws Exception {
        String email = emailOf(jwt);
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "NO_IDENTITY", "Inicia sesión para ver tus pedidos.");
        }
        String owner = ownerId(jwt);
        String raw = ordersServiceClient.get().uri("/api/orders").retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode list = root.isArray() ? root : root.path("content");

        ArrayNode mine = objectMapper.createArrayNode();
        for (JsonNode o : list) {
            boolean byOwner = owner.equals(o.path("clienteId").asText(""));
            boolean byLegacyEmail = email != null && email.equalsIgnoreCase(o.path("clienteEmail").asText(""));
            if (byOwner || byLegacyEmail) {
                mine.add(o);
            }
        }
        return ResponseEntity.ok(mine);
    }

    /** Identificador estable del dueño del pedido, derivado del token. */
    private static String ownerId(Jwt jwt) {
        return "U-" + jwt.getSubject();
    }

    /**
     * Correo real del usuario. Prefiere el claim "email". Las cuentas invitadas
     * (guest) traen en
     * preferred_username un UPN como
     * "usuario_dominio.cl#EXT#@tenant.onmicrosoft.com", que NO es un buzón:
     * se reconvierte a "usuario@dominio.cl". Si no hay un correo válido devuelve
     * null y se usa el del formulario.
     */
    private static String emailOf(Jwt jwt) {
        if (jwt == null)
            return null;
        for (String claim : new String[] { "email", "preferred_username", "upn" }) {
            String v = jwt.getClaimAsString(claim);
            if (v == null || v.isBlank())
                continue;
            int ext = v.indexOf("#EXT#");
            if (ext >= 0) {
                String left = v.substring(0, ext);
                int us = left.lastIndexOf('_');
                if (us <= 0)
                    continue;
                v = left.substring(0, us) + "@" + left.substring(us + 1);
            }
            if (v.contains("@") && !v.contains("#"))
                return v;
        }
        return null;
    }
}