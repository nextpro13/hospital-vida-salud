package pe.cibertec.hospital.pacientes.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(RecursoDuplicadoException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ResponseEntity<ErrorResponse> transicion(TransicionInvalidaException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> noDisponible(ServicioNoDisponibleException ex, ServerWebExchange exchange) {
        log.warn("Dependencia no disponible: {}", ex.getMessage());
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> errorRemoto(WebClientResponseException ex, ServerWebExchange exchange) {
        log.warn("Respuesta inesperada de un servicio remoto: {}", ex.getStatusCode());
        return construir(HttpStatus.BAD_GATEWAY, "Un servicio dependiente devolvió una respuesta inesperada", exchange, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.CONFLICT, "La operación viola una restricción de unicidad o integridad de datos", exchange, List.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> validacion(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> detalles = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", exchange, detalles);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> entradaInvalida(ServerWebInputException ex, ServerWebExchange exchange) {
        return construir(HttpStatus.BAD_REQUEST, "La solicitud está mal formada o tiene tipos de dato incorrectos", exchange, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> estadoHttp(ResponseStatusException ex, ServerWebExchange exchange) {
        return construir(ex.getStatusCode(), ex.getReason(), exchange, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, ServerWebExchange exchange) {
        log.error("Error no controlado en {}", exchange.getRequest().getPath(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intente nuevamente.", exchange, List.of());
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatusCode status, String mensaje,
                                                    ServerWebExchange exchange, List<String> detalles) {
        HttpStatus resuelto = HttpStatus.resolve(status.value());
        String error = resuelto != null ? resuelto.getReasonPhrase() : String.valueOf(status.value());
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), status.value(), error, mensaje,
                exchange.getRequest().getPath().value(), detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
