package pe.cibertec.hospital.atenciones.service;

import pe.cibertec.hospital.atenciones.client.CitaClient;
import pe.cibertec.hospital.atenciones.client.CitaResumen;
import pe.cibertec.hospital.atenciones.dto.AtencionRequest;
import pe.cibertec.hospital.atenciones.event.EventoDominio;
import pe.cibertec.hospital.atenciones.event.EventoPublisher;
import pe.cibertec.hospital.atenciones.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.atenciones.exception.ReglaNegocioException;
import pe.cibertec.hospital.atenciones.exception.ServicioNoDisponibleException;
import pe.cibertec.hospital.atenciones.exception.TransicionInvalidaException;
import pe.cibertec.hospital.atenciones.model.Atencion;
import pe.cibertec.hospital.atenciones.model.EstadoAtencion;
import pe.cibertec.hospital.atenciones.repository.AtencionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtencionServiceTest {
    @Mock
    private AtencionRepository repository;

    @Mock
    private CitaClient citaClient;

    @Mock
    private EventoPublisher publisher;

    private AtencionService service;

    private final AtencionRequest request = new AtencionRequest(10L, "Consulta externa", "Atención administrativa de prueba");

    @BeforeEach
    void setUp() {
        service = new AtencionService(repository, citaClient, publisher,
                Clock.fixed(Instant.parse("2026-10-12T14:30:00Z"), ZoneId.of("America/Lima")));
    }

    private CitaResumen cita(String estado) {
        return new CitaResumen(10L, "CIT-ABC12345", 1L, estado, LocalDateTime.of(2026, 10, 12, 9, 30));
    }

    @Test
    @DisplayName("Registra la atención de una cita CONFIRMADA y marca la cita como ATENDIDA")
    void registraAtencion() {
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("CONFIRMADA")));
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(repository.save(any(Atencion.class))).thenAnswer(inv -> {
            Atencion a = inv.getArgument(0);
            a.setId(3L);
            return Mono.just(a);
        });
        when(citaClient.marcarAtendida(10L)).thenReturn(Mono.empty());
        when(publisher.publicar(eq("atencion.registrada"), any(EventoDominio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request))
                .assertNext(a -> {
                    assertThat(a.getPacienteId()).isEqualTo(1L);
                    assertThat(a.getEstado()).isEqualTo(EstadoAtencion.EN_CURSO);
                    assertThat(a.getArea()).isEqualTo("CONSULTA EXTERNA");
                })
                .verifyComplete();

        verify(citaClient).marcarAtendida(10L);
    }

    @Test
    @DisplayName("Rechaza registrar atención de una cita que no está CONFIRMADA")
    void rechazaCitaPendiente() {
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("PENDIENTE")));

        StepVerifier.create(service.registrar(request))
                .expectError(ReglaNegocioException.class)
                .verify();

        verify(repository, never()).save(any(Atencion.class));
    }

    @Test
    @DisplayName("Rechaza una segunda atención para la misma cita")
    void rechazaAtencionDuplicada() {
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("CONFIRMADA")));
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(request))
                .expectError(RecursoDuplicadoException.class)
                .verify();
    }

    @Test
    @DisplayName("Compensación: si ms-citas falla al cerrar la cita, se elimina la atención guardada")
    void compensaSiNoSePuedeCerrarLaCita() {
        when(citaClient.obtenerCita(10L)).thenReturn(Mono.just(cita("CONFIRMADA")));
        when(repository.existsByCitaId(10L)).thenReturn(Mono.just(false));
        when(repository.save(any(Atencion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(citaClient.marcarAtendida(10L))
                .thenReturn(Mono.error(new ServicioNoDisponibleException("ms-citas caído", null)));
        when(repository.delete(any(Atencion.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(request))
                .expectError(ServicioNoDisponibleException.class)
                .verify();

        verify(repository).delete(any(Atencion.class));
        verify(publisher, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("Una atención FINALIZADA no puede anularse")
    void noAnulaAtencionFinalizada() {
        Atencion a = new Atencion();
        a.setId(3L);
        a.setCodigo("ATE-1");
        a.setEstado(EstadoAtencion.FINALIZADA);
        when(repository.findById(3L)).thenReturn(Mono.just(a));

        StepVerifier.create(service.cambiarEstado(3L, EstadoAtencion.ANULADA))
                .expectError(TransicionInvalidaException.class)
                .verify();
    }
}
