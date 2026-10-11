package pe.cibertec.hospital.pacientes.controller;

import pe.cibertec.hospital.pacientes.dto.PacienteRequest;
import pe.cibertec.hospital.pacientes.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.pacientes.model.EstadoPaciente;
import pe.cibertec.hospital.pacientes.model.Paciente;
import pe.cibertec.hospital.pacientes.service.PacienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PacienteController.class)
class PacienteControllerTest {
    private static final String JSON_VALIDO = """
            {
              "tipoDocumento": "DNI",
              "numeroDocumento": "70000001",
              "nombres": "Ana",
              "apellidos": "Prueba Ficticia",
              "fechaNacimiento": "1990-05-20",
              "telefono": "987654321",
              "correo": "ana.prueba@correo-ficticio.pe"
            }
            """;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PacienteService service;

    @Test
    @DisplayName("POST /api/pacientes devuelve 201 con el paciente creado")
    void registraPaciente() {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNombres("Ana");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        p.setEstado(EstadoPaciente.ACTIVO);
        when(service.registrar(any(PacienteRequest.class))).thenReturn(Mono.just(p));

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JSON_VALIDO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.estado").isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("POST /api/pacientes con documento repetido devuelve 409")
    void rechazaDuplicado() {
        when(service.registrar(any(PacienteRequest.class)))
                .thenReturn(Mono.error(new RecursoDuplicadoException("Ya existe un paciente con ese documento")));

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JSON_VALIDO)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.mensaje").isEqualTo("Ya existe un paciente con ese documento");
    }

    @Test
    @DisplayName("POST /api/pacientes con datos inválidos devuelve 400 y no llama al servicio")
    void rechazaDatosInvalidos() {
        String invalido = """
                { "tipoDocumento": "DNI", "numeroDocumento": "70000001", "correo": "no-es-correo" }
                """;

        webTestClient.post().uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalido)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detalles").isArray();

        verify(service, never()).registrar(any(PacienteRequest.class));
    }

    @Test
    @DisplayName("GET /api/pacientes/{id} inexistente devuelve 404")
    void pacienteInexistente() {
        when(service.buscarPorId(99L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("No existe el paciente con id 99")));

        webTestClient.get().uri("/api/pacientes/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/api/pacientes/99");
    }
}
