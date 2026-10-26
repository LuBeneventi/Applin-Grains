package cl.duoc.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class ApiProblemHandler {

    private final ObjectMapper mapper;

    public ApiProblemHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<Map<String, String>> handle(ApiProblem p) {
        return ResponseEntity.status(p.getStatus()).body(Map.of("error", p.getMessage(), "code", p.getCode()));
    }

    /**
     * Errores de los microservicios (400 stock, 404, 409 transición de estado no permitida…):
     * se reenvían con su estado y mensaje, en vez de convertirse en un 500 genérico.
     */
    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<Map<String, String>> upstream(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String message = e.getStatusText();
        try {
            JsonNode body = mapper.readTree(e.getResponseBodyAsString());
            String m = body.path("message").asText(body.path("error").asText(""));
            if (!m.isBlank()) message = m;
        } catch (Exception ignored) {
            // la respuesta no era JSON: se usa el texto del estado
        }
        HttpStatus resolved = HttpStatus.resolve(status);
        return ResponseEntity.status(resolved != null ? resolved : HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message, "code", "UPSTREAM_" + status));
    }
}
