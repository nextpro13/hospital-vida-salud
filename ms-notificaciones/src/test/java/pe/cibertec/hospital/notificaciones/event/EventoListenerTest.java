package pe.cibertec.hospital.notificaciones.event;

import pe.cibertec.hospital.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoListenerTest {
    @Test
    @DisplayName("El listener entrega el evento recibido al servicio de notificaciones")
    void delegaEnElServicio() {
        NotificacionService service = mock(NotificacionService.class);
        EventoListener listener = new EventoListener(service);
        EventoDominio evento = EventoDominio.de("CitaConfirmada", "CITA", "10", "PENDIENTE", "CONFIRMADA",
                "ms-citas", Map.of("pacienteId", 1));
        when(service.procesar(evento)).thenReturn(Mono.empty());

        listener.recibir(evento);

        verify(service).procesar(evento);
    }
}
