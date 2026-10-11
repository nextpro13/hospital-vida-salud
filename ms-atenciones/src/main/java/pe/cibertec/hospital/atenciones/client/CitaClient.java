package pe.cibertec.hospital.atenciones.client;

import pe.cibertec.hospital.atenciones.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.atenciones.exception.ReglaNegocioException;
import pe.cibertec.hospital.atenciones.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class CitaClient {
    private final WebClient webClient;
    private final Duration timeout;

    public CitaClient(@LoadBalanced WebClient.Builder builder,
                      @Value("${clientes.citas.url:http://ms-citas}") String baseUrl,
                      @Value("${clientes.citas.timeout-ms:2000}") long timeoutMs) {
        this.webClient = builder.clone().baseUrl(baseUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public Mono<CitaResumen> obtenerCita(Long citaId) {
        return webClient.get()
                .uri("/api/citas/{id}", citaId)
                .retrieve()
                .onStatus(s -> s.value() == 404, r -> Mono.error(
                        new RecursoNoEncontradoException("La cita " + citaId + " no existe")))
                .bodyToMono(CitaResumen.class)
                .timeout(timeout)
                .onErrorMap(CitaClient::esTransitorio, ex -> new ServicioNoDisponibleException(
                        "El servicio de citas no está disponible en este momento", ex));
    }

    public Mono<Void> marcarAtendida(Long citaId) {
        return webClient.patch()
                .uri("/api/citas/{id}/atender", citaId)
                .retrieve()
                .onStatus(s -> s.value() == 409, r -> Mono.error(
                        new ReglaNegocioException("La cita " + citaId + " no admite el cambio a ATENDIDA")))
                .toBodilessEntity()
                .timeout(timeout)
                .onErrorMap(CitaClient::esTransitorio, ex -> new ServicioNoDisponibleException(
                        "El servicio de citas no está disponible en este momento", ex))
                .then();
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException r && r.getStatusCode().is5xxServerError());
    }
}
