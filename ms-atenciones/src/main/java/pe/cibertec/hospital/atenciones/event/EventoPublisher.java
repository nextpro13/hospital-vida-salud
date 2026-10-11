package pe.cibertec.hospital.atenciones.event;

import pe.cibertec.hospital.atenciones.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class EventoPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public Mono<Void> publicar(String routingKey, EventoDominio evento) {
        return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, evento))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("Evento publicado: tipo={} routingKey={} recursoId={} eventoId={}",
                        evento.tipo(), routingKey, evento.recursoId(), evento.eventoId()))
                .then()
                .onErrorResume(ex -> {
                    log.error("No se pudo publicar el evento {} ({}): {}", evento.tipo(), routingKey, ex.getMessage());
                    return Mono.empty();
                });
    }
}
