package cl.duoc.api;

import org.springframework.http.HttpStatus;

/** Error de negocio con estado HTTP y código. Se responde como { "error": "...", "code": "..." }. */
public class ApiProblem extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiProblem(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
