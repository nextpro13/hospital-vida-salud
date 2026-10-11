package pe.cibertec.hospital.pacientes.service;

import pe.cibertec.hospital.pacientes.dto.PacienteRequest;
import pe.cibertec.hospital.pacientes.event.EventoDominio;
import pe.cibertec.hospital.pacientes.event.EventoPublisher;
import pe.cibertec.hospital.pacientes.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.pacientes.exception.ReglaNegocioException;
import pe.cibertec.hospital.pacientes.model.EstadoPaciente;
import pe.cibertec.hospital.pacientes.model.Paciente;
import pe.cibertec.hospital.pacientes.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {
    @Mock
    private PacienteRepository repository;

    @Mock
    private EventoPublisher publisher;

    private PacienteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Lima"));
        service = new PacienteService(repository, publisher, clock);
    }

    private PacienteRequest solicitud(String numeroDocumento) {
        return new PacienteRequest("DNI", numeroDocumento, "Ana", "Prueba Ficticia",
                LocalDate.of(1990, 5, 20), "987654321", "Ana.Prueba@correo-ficticio.pe", "Av. Ejemplo 123");
    }

    @Test
    @DisplayName("Registra un paciente válido como ACTIVO y publica PacienteRegistrado")
    void registraPacienteValido() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento("DNI", "70000001")).thenReturn(Mono.just(false));
        when(repository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(1L);
            return Mono.just(p);
        });
        when(publisher.publicar(eq("paciente.registrado"), any(EventoDominio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(solicitud("70000001")))
                .assertNext(p -> {
                    assertThat(p.getId()).isEqualTo(1L);
                    assertThat(p.getEstado()).isEqualTo(EstadoPaciente.ACTIVO);
                    assertThat(p.getCorreo()).isEqualTo("ana.prueba@correo-ficticio.pe");
                    assertThat(p.getFechaRegistro()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<EventoDominio> evento = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publisher).publicar(eq("paciente.registrado"), evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("PacienteRegistrado");
        assertThat(evento.getValue().recursoId()).isEqualTo("1");
    }

    @Test
    @DisplayName("Rechaza un paciente con documento duplicado y no lo guarda")
    void rechazaPacienteDuplicado() {
        when(repository.existsByTipoDocumentoAndNumeroDocumento("DNI", "70000001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(solicitud("70000001")))
                .expectError(RecursoDuplicadoException.class)
                .verify();

        verify(repository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Rechaza un DNI que no tiene 8 dígitos")
    void rechazaDniInvalido() {
        StepVerifier.create(service.registrar(solicitud("12345A789")))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    @DisplayName("Devuelve error de recurso inexistente al buscar un id que no existe")
    void buscarPacienteInexistente() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.buscarPorId(99L))
                .expectErrorMatches(e -> e instanceof RecursoNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();
    }

    @Test
    @DisplayName("No permite cambiar al mismo estado que ya tiene el paciente")
    void noCambiaAlMismoEstado() {
        Paciente p = new Paciente();
        p.setId(5L);
        p.setEstado(EstadoPaciente.ACTIVO);
        when(repository.findById(5L)).thenReturn(Mono.just(p));

        StepVerifier.create(service.cambiarEstado(5L, EstadoPaciente.ACTIVO))
                .expectError(ReglaNegocioException.class)
                .verify();
    }
}
