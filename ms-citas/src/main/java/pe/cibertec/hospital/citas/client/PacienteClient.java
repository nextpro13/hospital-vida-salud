package pe.cibertec.hospital.citas.client;

import pe.cibertec.hospital.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.citas.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class PacienteClient {
    private final WebClient webClient;
    private final Duration timeout;
    private final int reintentos;

    public PacienteClient(@LoadBalanced WebClient.Builder builder,
                          @Value("${clientes.pacientes.url:http://ms-pacientes}") String baseUrl,
                          @Value("${clientes.pacientes.timeout-ms:2000}") long timeoutMs,
                          @Value("${clientes.pacientes.reintentos:2}") int reintentos) {
        this.webClient = builder.clone().baseUrl(baseUrl).build();
        this.timeout = Duration.ofMillis(timeoutMs);
        this.reintentos = reintentos;
    }

    public Mono<PacienteResumen> obtenerPaciente(Long pacienteId) {
        return webClient.get()
                .uri("/api/pacientes/{id}", pacienteId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        respuesta -> Mono.error(new RecursoNoEncontradoException(
                                "El paciente " + pacienteId + " no existe en el servicio de pacientes")))
                .bodyToMono(PacienteResumen.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(reintentos, Duration.ofMillis(200))
                        .filter(PacienteClient::esTransitorio)
                        .onRetryExhaustedThrow((spec, senal) -> senal.failure()))
                .onErrorMap(PacienteClient::esTransitorio, ex -> new ServicioNoDisponibleException(
                        "El servicio de pacientes no está disponible en este momento", ex));
    }

    static boolean esTransitorio(Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof WebClientRequestException
                || (ex instanceof WebClientResponseException respuesta && respuesta.getStatusCode().is5xxServerError());
    }
}
