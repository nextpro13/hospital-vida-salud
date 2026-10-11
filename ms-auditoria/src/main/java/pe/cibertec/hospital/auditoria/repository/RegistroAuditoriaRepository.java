package pe.cibertec.hospital.auditoria.repository;

import pe.cibertec.hospital.auditoria.model.RegistroAuditoria;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RegistroAuditoriaRepository extends R2dbcRepository<RegistroAuditoria, Long> {
    Mono<Boolean> existsByEventoId(String eventoId);

    Flux<RegistroAuditoria> findByRecursoAndRecursoIdOrderByFechaEventoAsc(String recurso, String recursoId);

    Flux<RegistroAuditoria> findByTipoEventoOrderByFechaEventoDesc(String tipoEvento);
}
