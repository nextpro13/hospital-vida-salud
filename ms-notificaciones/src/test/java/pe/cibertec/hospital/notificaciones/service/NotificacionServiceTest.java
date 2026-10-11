package pe.cibertec.hospital.notificaciones.service;

import pe.cibertec.hospital.notificaciones.event.EventoDominio;
import pe.cibertec.hospital.notificaciones.model.Notificacion;
import pe.cibertec.hospital.notificaciones.repository.NotificacionRepository;
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
class NotificacionServiceTest {
    @Mock
    private NotificacionRepository repository;

    private NotificacionService service;

    private final EventoDominio citaConfirmada = EventoDominio.de("CitaConfirmada", "CITA", "10", "PENDIENTE",
            "CONFIRMADA", "ms-citas", Map.of("pacienteId", 1, "codigo", "CIT-ABC12345",
                    "especialidad", "CARDIOLOGIA", "fechaHora", "2026-10-12T09:30"));

    @BeforeEach
    void setUp() {
        service = new NotificacionService(repository, Clock.systemUTC());
    }

    @Test
    @DisplayName("Consume CitaConfirmada y guarda una notificación simulada para el paciente")
    void generaNotificacionDeCitaConfirmada() {
        when(repository.existsByEventoId(citaConfirmada.eventoId())).thenReturn(Mono.just(false));
        when(repository.save(any(Notificacion.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.procesar(citaConfirmada))
                .assertNext(n -> {
                    assertThat(n.getPacienteId()).isEqualTo(1L);
                    assertThat(n.getTipoEvento()).isEqualTo("CitaConfirmada");
                    assertThat(n.getMensaje()).contains("CIT-ABC12345").contains("CONFIRMADA").contains("2026-10-12 09:30");
                    assertThat(n.getEstado()).isEqualTo("ENVIADA_SIMULADA");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Es idempotente: un evento repetido no genera otra notificación")
    void ignoraEventoDuplicado() {
        when(repository.existsByEventoId(citaConfirmada.eventoId())).thenReturn(Mono.just(true));

        StepVerifier.create(service.procesar(citaConfirmada)).verifyComplete();

        verify(repository, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("Arma el mensaje de pago registrado")
    void mensajeDePago() {
        EventoDominio pago = EventoDominio.de("PagoRegistrado", "PAGO", "1", null, "REGISTRADO", "ms-pagos",
                Map.of("pacienteId", 1, "monto", "80.00", "numeroOperacion", "OP-TEST-0001"));

        assertThat(NotificacionService.mensaje(pago)).contains("S/ 80.00").contains("OP-TEST-0001");
    }
}
