package pe.cibertec.hospital.citas.client;

import pe.cibertec.hospital.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.citas.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PacienteClientTest {
    private PacienteClient cliente(ExchangeFunction respuesta) {
        return new PacienteClient(WebClient.builder().exchangeFunction(respuesta), "http://ms-pacientes", 300, 1);
    }

    @Test
    @DisplayName("200: devuelve el resumen del paciente")
    void pacienteExiste() {
        PacienteClient cliente = cliente(req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"id\":1,\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"estado\":\"ACTIVO\",\"telefono\":\"999\"}")
                .build()));

        StepVerifier.create(cliente.obtenerPaciente(1L))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo(1L);
                    assertThat(p.estaActivo()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("404: se traduce a RecursoNoEncontradoException")
    void pacienteNoExiste() {
        PacienteClient cliente = cliente(req -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build()));

        StepVerifier.create(cliente.obtenerPaciente(99L))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    @DisplayName("Sin respuesta dentro del tiempo de espera: ServicioNoDisponibleException")
    void servicioNoResponde() {
        PacienteClient cliente = cliente(req -> Mono.never());

        StepVerifier.create(cliente.obtenerPaciente(1L))
                .expectError(ServicioNoDisponibleException.class)
                .verify();
    }

    @Test
    @DisplayName("503 del servicio remoto: ServicioNoDisponibleException")
    void servicioCaido() {
        PacienteClient cliente = cliente(req -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()));

        StepVerifier.create(cliente.obtenerPaciente(1L))
                .expectError(ServicioNoDisponibleException.class)
                .verify();
    }
}
