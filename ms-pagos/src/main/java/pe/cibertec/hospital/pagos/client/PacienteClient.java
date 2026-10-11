package pe.cibertec.hospital.pagos.client;

import pe.cibertec.hospital.pagos.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.pagos.exception.ServicioNoDisponibleException;
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
public class PacienteClient {
    private final WebClient webClient;
    private final Duration timeout;

    public PacienteClient(@LoadBalanced WebClient.Builder builder,
                          @Value("${clientes.pacientes.url:http://ms-pacientes}") String baseUrl,
                          @Value("${clientes.pacientes.timeout-ms:2000}") long timeoutMs) {
        this.webClient = builder.clone().baseUrl(baseUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public Mono<Void> verificarExistencia(Long pacienteId) {
        return webClient.get()
                .uri("/api/pacientes/{id}", pacienteId)
                .retrieve()
                .onStatus(s -> s.value() == 404, r -> Mono.error(
                        new RecursoNoEncontradoException("El paciente " + pacienteId + " no existe")))
                .toBodilessEntity()
                .timeout(timeout)
                .onErrorMap(PacienteClient::esTransitorio, ex -> new ServicioNoDisponibleException(
                        "El servicio de pacientes no está disponible en este momento", ex))
                .then();
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException r && r.getStatusCode().is5xxServerError());
    }
}
