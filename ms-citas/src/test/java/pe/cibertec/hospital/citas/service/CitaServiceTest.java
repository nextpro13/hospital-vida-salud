package pe.cibertec.hospital.citas.service;

import pe.cibertec.hospital.citas.client.PacienteClient;
import pe.cibertec.hospital.citas.client.PacienteResumen;
import pe.cibertec.hospital.citas.dto.CitaRequest;
import pe.cibertec.hospital.citas.event.EventoDominio;
import pe.cibertec.hospital.citas.event.EventoPublisher;
import pe.cibertec.hospital.citas.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.citas.exception.ReglaNegocioException;
import pe.cibertec.hospital.citas.exception.TransicionInvalidaException;
import pe.cibertec.hospital.citas.model.Cita;
import pe.cibertec.hospital.citas.model.EstadoCita;
import pe.cibertec.hospital.citas.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDateTime LUNES_0930 = LocalDateTime.of(2026, 10, 12, 9, 30);

    @Mock
    private CitaRepository repository;

    @Mock
    private PacienteClient pacienteClient;

    @Mock
    private EventoPublisher publisher;

    private CitaService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 10, 10, 0, 0, 0, LIMA).toInstant(), LIMA);
        service = new CitaService(repository, pacienteClient, publisher, clock);
    }

    private CitaRequest solicitud() {
        return new CitaRequest(1L, "Cardiologia", LUNES_0930, "MED-001", "Control administrativo");
    }

    private Cita cita(Long id, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(id);
        c.setCodigo("CIT-TEST" + id);
        c.setPacienteId(1L);
        c.setEspecialidad("CARDIOLOGIA");
        c.setFechaHora(LUNES_0930);
        c.setProfesionalCodigo("MED-001");
        c.setEstado(estado);
        return c;
    }

    @Test
    @DisplayName("Registra una cita PENDIENTE para un paciente válido (comunicación síncrona simulada)")
    void registraCitaValida() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(new PacienteResumen(1L, "Ana", "Prueba", "ACTIVO")));
        when(repository.findByProfesionalCodigoAndFechaHora("MED-001", LUNES_0930)).thenReturn(Flux.empty());
        when(repository.findByPacienteIdAndFechaHora(1L, LUNES_0930)).thenReturn(Flux.empty());
        when(repository.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setId(10L);
            return Mono.just(c);
        });
        when(publisher.publicar(eq("cita.creada"), any(EventoDominio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(solicitud()))
                .assertNext(c -> {
                    assertThat(c.getId()).isEqualTo(10L);
                    assertThat(c.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
                    assertThat(c.getCodigo()).startsWith("CIT-");
                    assertThat(c.getEspecialidad()).isEqualTo("CARDIOLOGIA");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("No registra la cita si el paciente no existe")
    void rechazaPacienteInexistente() {
        when(pacienteClient.obtenerPaciente(1L))
                .thenReturn(Mono.error(new RecursoNoEncontradoException("El paciente 1 no existe")));

        StepVerifier.create(service.registrar(solicitud()))
                .expectError(RecursoNoEncontradoException.class)
                .verify();

        verify(repository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("No registra la cita si el paciente está INACTIVO")
    void rechazaPacienteInactivo() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(new PacienteResumen(1L, "Ana", "Prueba", "INACTIVO")));

        StepVerifier.create(service.registrar(solicitud()))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    @DisplayName("Rechaza una doble reserva del mismo profesional y horario")
    void rechazaHorarioOcupado() {
        when(pacienteClient.obtenerPaciente(1L)).thenReturn(Mono.just(new PacienteResumen(1L, "Ana", "Prueba", "ACTIVO")));
        when(repository.findByProfesionalCodigoAndFechaHora("MED-001", LUNES_0930))
                .thenReturn(Flux.just(cita(7L, EstadoCita.CONFIRMADA)));
        when(repository.findByPacienteIdAndFechaHora(1L, LUNES_0930)).thenReturn(Flux.empty());

        StepVerifier.create(service.registrar(solicitud()))
                .expectError(RecursoDuplicadoException.class)
                .verify();

        verify(repository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Confirma una cita PENDIENTE y publica CitaConfirmada con el estado anterior")
    void confirmaCitaPendiente() {
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.PENDIENTE)));
        when(repository.save(any(Cita.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publisher.publicar(eq("cita.confirmada"), any(EventoDominio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(10L, EstadoCita.CONFIRMADA))
                .assertNext(c -> assertThat(c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA))
                .verifyComplete();

        ArgumentCaptor<EventoDominio> captor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(publisher).publicar(eq("cita.confirmada"), captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo("CitaConfirmada");
        assertThat(captor.getValue().estadoAnterior()).isEqualTo("PENDIENTE");
        assertThat(captor.getValue().estadoNuevo()).isEqualTo("CONFIRMADA");
        assertThat(captor.getValue().datos()).containsEntry("pacienteId", 1L);
    }

    @Test
    @DisplayName("Transición no permitida: no se confirma una cita CANCELADA")
    void noConfirmaCitaCancelada() {
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.CANCELADA)));

        StepVerifier.create(service.cambiarEstado(10L, EstadoCita.CONFIRMADA))
                .expectError(TransicionInvalidaException.class)
                .verify();

        verify(repository, never()).save(any(Cita.class));
        verify(publisher, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("No se marca como atendida una cita que no existe")
    void noAtiendeCitaInexistente() {
        when(repository.findById(404L)).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(404L, EstadoCita.ATENDIDA))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    @DisplayName("Consulta una cita por id")
    void consultaCita() {
        when(repository.findById(10L)).thenReturn(Mono.just(cita(10L, EstadoCita.PENDIENTE)));

        StepVerifier.create(service.buscarPorId(10L))
                .assertNext(c -> assertThat(c.getCodigo()).isEqualTo("CIT-TEST10"))
                .verifyComplete();
    }
}
