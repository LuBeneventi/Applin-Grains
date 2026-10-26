package cl.duoc.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pasarela de pago SIMULADA. Valida de verdad (monto desde catálogo, stock,
 * Luhn, vencimiento, CVV),
 * pero no mueve dinero. Tarjetas de prueba:
 * 4242 4242 4242 4242 -> aprobada 4000 0000 0000 0002 -> rechazada 4000 0000
 * 0000 9995 -> sin fondos
 * Nunca se guarda ni se registra el número completo de la tarjeta.
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final CartPricer pricer;
    private final PaymentStore store;
    private final ObjectMapper objectMapper;

    public PaymentController(CartPricer pricer, PaymentStore store, ObjectMapper objectMapper) {
        this.pricer = pricer;
        this.store = store;
        this.objectMapper = objectMapper;
    }

    // El cuerpo llega como String y se parsea a mano (igual que
    // CheckoutController):
    // así no depende del conversor JSON de Spring (Jackson 2 vs 3 en Spring Boot
    // 4).
    @PostMapping(value = "/api/payments/charge", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> charge(@RequestBody String raw, @AuthenticationPrincipal Jwt jwt) {
        JsonNode body;
        try {
            body = objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw invalid("INVALID_BODY", "La solicitud de pago no es válida.");
        }

        // El monto se calcula aquí, desde el catálogo: nunca viene del cliente
        CartPricer.PricedCart cart = pricer.price(body.get("items"));

        JsonNode card = body.path("card");
        String number = card.path("number").asText("").replaceAll("\\D", "");
        String name = card.path("name").asText("").trim();
        String expiry = card.path("expiry").asText("");
        String cvv = card.path("cvv").asText("");

        if (!luhn(number))
            throw invalid("INVALID_CARD", "El número de tarjeta no es válido.");
        if (name.length() < 3)
            throw invalid("INVALID_NAME", "Falta el nombre que aparece en la tarjeta.");
        if (!validExpiry(expiry))
            throw invalid("INVALID_EXPIRY", "La tarjeta está vencida o la fecha no es válida.");
        if (!cvv.matches("\\d{3,4}"))
            throw invalid("INVALID_CVV", "El código de seguridad no es válido.");

        // Respuestas simuladas del banco
        if (number.equals("4000000000000002")) {
            throw new ApiProblem(HttpStatus.PAYMENT_REQUIRED, "CARD_DECLINED",
                    "Tu banco rechazó la tarjeta. Prueba con otro medio de pago.");
        }
        if (number.equals("4000000000009995")) {
            throw new ApiProblem(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_FUNDS",
                    "Fondos insuficientes en la tarjeta.");
        }

        String last4 = number.substring(number.length() - 4);
        String paymentId = store.authorize(PaymentStore.ownerOf(jwt), cart);
        log.info("Pago simulado aprobado {} monto={} tarjeta=****{}", paymentId, cart.total(), last4);

        return Map.of(
                "paymentId", paymentId,
                "status", "APPROVED",
                "authorizationCode", String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000)),
                "amount", cart.total(),
                "brand", brandOf(number),
                "last4", last4);
    }

    private static ApiProblem invalid(String code, String message) {
        return new ApiProblem(HttpStatus.BAD_REQUEST, code, message);
    }

    private static boolean luhn(String d) {
        if (d.length() < 13 || d.length() > 19)
            return false;
        int sum = 0;
        boolean alt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int x = d.charAt(i) - '0';
            if (alt) {
                x *= 2;
                if (x > 9)
                    x -= 9;
            }
            sum += x;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private static boolean validExpiry(String v) {
        if (!v.matches("\\d{2}/\\d{2}"))
            return false;
        try {
            YearMonth exp = YearMonth.of(2000 + Integer.parseInt(v.substring(3)), Integer.parseInt(v.substring(0, 2)));
            YearMonth now = YearMonth.now();
            return !exp.isBefore(now) && !exp.isAfter(now.plusYears(10));
        } catch (Exception e) {
            return false;
        }
    }

    private static String brandOf(String n) {
        if (n.startsWith("4"))
            return "VISA";
        if (n.matches("^(5[1-5]|2[2-7]).*"))
            return "MASTERCARD";
        return "TARJETA";
    }
}