package pe.cibertec.hospital.auditoria.event;

import pe.cibertec.hospital.auditoria.config.RabbitConfig;
import pe.cibertec.hospital.auditoria.service.AuditoriaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EventoListener {
    private final AuditoriaService service;

    public EventoListener(AuditoriaService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.COLA)
    public void recibir(EventoDominio evento) {
        service.registrar(evento).block(Duration.ofSeconds(10));
    }
}
