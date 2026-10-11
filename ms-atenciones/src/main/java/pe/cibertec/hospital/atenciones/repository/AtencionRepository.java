package pe.cibertec.hospital.atenciones.repository;

import pe.cibertec.hospital.atenciones.model.Atencion;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AtencionRepository extends R2dbcRepository<Atencion, Long> {
    Mono<Boolean> existsByCitaId(Long citaId);

    Mono<Atencion> findByCitaId(Long citaId);

    Flux<Atencion> findByPacienteIdOrderByFechaAtencionDesc(Long pacienteId);
}
