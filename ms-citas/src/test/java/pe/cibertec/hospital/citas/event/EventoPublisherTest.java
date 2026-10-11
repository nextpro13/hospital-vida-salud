package pe.cibertec.hospital.citas.event;

import pe.cibertec.hospital.citas.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventoPublisherTest {
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final EventoPublisher publisher = new EventoPublisher(rabbitTemplate);

    private final EventoDominio evento = EventoDominio.de("CitaConfirmada", "CITA", "10", "PENDIENTE",
            "CONFIRMADA", "ms-citas", Map.of("pacienteId", 1L));

    @Test
    @DisplayName("Publica CitaConfirmada en el exchange hospital.eventos con routing key cita.confirmada")
    void publicaEnElExchangeCorrecto() {
        StepVerifier.create(publisher.publicar("cita.confirmada", evento)).verifyComplete();

        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE, "cita.confirmada", (Object) evento);
    }

    @Test
    @DisplayName("Si el broker falla, la operación principal no se interrumpe")
    void toleraFallaDelBroker() {
        doThrow(new RuntimeException("Broker no disponible"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        StepVerifier.create(publisher.publicar("cita.confirmada", evento)).verifyComplete();
    }
}
