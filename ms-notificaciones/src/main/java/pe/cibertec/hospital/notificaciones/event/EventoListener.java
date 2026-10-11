package pe.cibertec.hospital.notificaciones.event;

import pe.cibertec.hospital.notificaciones.config.RabbitConfig;
import pe.cibertec.hospital.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EventoListener {
    private static final Logger log = LoggerFactory.getLogger(EventoListener.class);
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);

    private final NotificacionService service;

    public EventoListener(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.COLA)
    public void recibir(EventoDominio evento) {
        log.info("Evento recibido en {}: tipo={} recursoId={} eventoId={}",
                RabbitConfig.COLA, evento.tipo(), evento.recursoId(), evento.eventoId());
        service.procesar(evento).block(ESPERA_MAXIMA);
    }
}
