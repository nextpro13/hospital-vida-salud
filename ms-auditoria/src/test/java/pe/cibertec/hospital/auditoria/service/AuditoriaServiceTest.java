package pe.cibertec.hospital.auditoria.service;

import pe.cibertec.hospital.auditoria.event.EventoDominio;
import pe.cibertec.hospital.auditoria.model.RegistroAuditoria;
import pe.cibertec.hospital.auditoria.repository.RegistroAuditoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {
    @Mock
    private RegistroAuditoriaRepository repository;

    private AuditoriaService service;

    private final EventoDominio evento = EventoDominio.de("CitaConfirmada", "CITA", "10", "PENDIENTE",
            "CONFIRMADA", "ms-citas", Map.of("pacienteId", 1));

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(repository, new ObjectMapper().findAndRegisterModules(), Clock.systemUTC());
    }

    @Test
    @DisplayName("Registra fecha, recurso, estado anterior, estado nuevo y origen del cambio")
    void registraTrazabilidad() {
        when(repository.existsByEventoId(evento.eventoId())).thenReturn(Mono.just(false));
        when(repository.save(any(RegistroAuditoria.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.registrar(evento))
                .assertNext(r -> {
                    assertThat(r.getRecurso()).isEqualTo("CITA");
                    assertThat(r.getRecursoId()).isEqualTo("10");
                    assertThat(r.getEstadoAnterior()).isEqualTo("PENDIENTE");
                    assertThat(r.getEstadoNuevo()).isEqualTo("CONFIRMADA");
                    assertThat(r.getOrigen()).isEqualTo("ms-citas");
                    assertThat(r.getFechaEvento()).isNotNull();
                    assertThat(r.getDetalle()).contains("pacienteId");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("No duplica registros si el mismo evento llega dos veces")
    void idempotente() {
        when(repository.existsByEventoId(evento.eventoId())).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(evento)).verifyComplete();

        verify(repository, never()).save(any(RegistroAuditoria.class));
    }
}
