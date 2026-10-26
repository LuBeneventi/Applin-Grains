package cl.duoc.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registro en memoria de pagos autorizados (simulación). Funciona como autorización/captura:
 * charge() autoriza y POST /api/orders captura (consume) el pago, una sola vez.
 * Al reiniciar el BFF se pierde; para producción iría en base de datos (o la pasarela real).
 */
@Component
public class PaymentStore {

    private static final Duration TTL = Duration.ofMinutes(15);

    private record Entry(String owner, BigDecimal amount, Map<Long, Integer> items, Instant createdAt, AtomicBoolean used) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    /** Identifica al dueño del pago: el sub del token, o "anonymous" si la seguridad está apagada. */
    public static String ownerOf(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "anonymous";
    }

    public String authorize(String owner, CartPricer.PricedCart cart) {
        purgeExpired();
        String id = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        entries.put(id, new Entry(owner, cart.total(), cart.quantities(), Instant.now(), new AtomicBoolean(false)));
        return id;
    }

    public void consume(String id, String owner, CartPricer.PricedCart cart) {
        Entry e = id == null ? null : entries.get(id);
        if (e == null || Instant.now().isAfter(e.createdAt().plus(TTL))) {
            throw new ApiProblem(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_NOT_FOUND", "No encontramos un pago aprobado para este pedido.");
        }
        if (!e.owner().equals(owner)) {
            throw new ApiProblem(HttpStatus.FORBIDDEN, "PAYMENT_OWNER", "Este pago pertenece a otro usuario.");
        }
        if (e.amount().compareTo(cart.total()) != 0 || !e.items().equals(cart.quantities())) {
            throw new ApiProblem(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_MISMATCH", "El pago no coincide con el contenido del pedido.");
        }
        if (!e.used().compareAndSet(false, true)) {
            throw new ApiProblem(HttpStatus.CONFLICT, "PAYMENT_USED", "Este pago ya fue utilizado.");
        }
    }

    /** Si el pedido no pudo crearse, el pago queda disponible para reintentar (no se cobró). */
    public void release(String id) {
        Entry e = id == null ? null : entries.get(id);
        if (e != null) {
            e.used().set(false);
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        entries.values().removeIf(e -> now.isAfter(e.createdAt().plus(TTL)));
    }
}
