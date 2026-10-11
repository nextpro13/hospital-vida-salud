package pe.cibertec.hospital.citas.controller;

import pe.cibertec.hospital.citas.dto.CitaRequest;
import pe.cibertec.hospital.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.citas.exception.ServicioNoDisponibleException;
import pe.cibertec.hospital.citas.exception.TransicionInvalidaException;
import pe.cibertec.hospital.citas.model.Cita;
import pe.cibertec.hospital.citas.model.EstadoCita;
import pe.cibertec.hospital.citas.service.CitaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CitaController.class)
class CitaControllerTest {
    private static final String JSON_CITA = """
            {
              "pacienteId": 1,
              "especialidad": "Cardiologia",
              "fechaHora": "2030-01-07T09:30:00",
              "profesionalCodigo": "MED-001",
              "motivo": "Control administrativo"
            }
            """;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CitaService service;

    private Cita cita(EstadoCita estado) {
        Cita c = new Cita();
        c.setId(10L);
        c.setCodigo("CIT-ABC12345");
        c.setPacienteId(1L);
        c.setFechaHora(LocalDateTime.of(2030, 1, 7, 9, 30));
        c.setEstado(estado);
        return c;
    }

    @Test
    @DisplayName("POST /api/citas devuelve 201")
    void registraCita() {
        when(service.registrar(any(CitaRequest.class))).thenReturn(Mono.just(cita(EstadoCita.PENDIENTE)));

        webTestClient.post().uri("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JSON_CITA)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CIT-ABC12345")
                .jsonPath("$.estado").isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("GET /api/citas/paciente/{id} devuelve la lista (Flux)")
    void listaCitasDelPaciente() {
        when(service.listarPorPaciente(1L)).thenReturn(Flux.just(cita(EstadoCita.PENDIENTE), cita(EstadoCita.CONFIRMADA)));

        webTestClient.get().uri("/api/citas/paciente/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("PATCH confirmar sobre una cita cancelada devuelve 409")
    void transicionNoPermitida() {
        when(service.cambiarEstado(10L, EstadoCita.CONFIRMADA))
                .thenReturn(Mono.error(new TransicionInvalidaException("La cita no puede pasar de CANCELADA a CONFIRMADA")));

        webTestClient.patch().uri("/api/citas/10/confirmar")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("GET de una cita inexistente devuelve 404")
    void citaInexistente() {
        when(service.buscarPorId(99L)).thenReturn(Mono.error(new RecursoNoEncontradoException("No existe la cita con id 99")));

        webTestClient.get().uri("/api/citas/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Si ms-pacientes no responde, POST /api/citas devuelve 503")
    void pacientesNoDisponible() {
        when(service.registrar(any(CitaRequest.class)))
                .thenReturn(Mono.error(new ServicioNoDisponibleException("El servicio de pacientes no está disponible", null)));

        webTestClient.post().uri("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JSON_CITA)
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    @DisplayName("POST /api/citas sin campos obligatorios devuelve 400")
    void datosIncompletos() {
        webTestClient.post().uri("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"especialidad\": \"Cardiologia\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
