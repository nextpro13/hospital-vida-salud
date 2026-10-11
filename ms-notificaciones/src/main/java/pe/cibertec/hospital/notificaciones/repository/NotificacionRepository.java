package pe.cibertec.hospital.notificaciones.repository;

import pe.cibertec.hospital.notificaciones.model.Notificacion;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificacionRepository extends R2dbcRepository<Notificacion, Long> {
    Mono<Boolean> existsByEventoId(String eventoId);

    Flux<Notificacion> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);
}
