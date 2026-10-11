package pe.cibertec.hospital.citas.repository;

import pe.cibertec.hospital.citas.model.Cita;
import pe.cibertec.hospital.citas.model.EstadoCita;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CitaRepository extends R2dbcRepository<Cita, Long> {
    Mono<Cita> findByCodigo(String codigo);

    Flux<Cita> findByPacienteIdOrderByFechaHoraAsc(Long pacienteId);

    Flux<Cita> findByEstadoOrderByFechaHoraAsc(EstadoCita estado);

    Flux<Cita> findByProfesionalCodigoAndFechaHora(String profesionalCodigo, LocalDateTime fechaHora);

    Flux<Cita> findByPacienteIdAndFechaHora(Long pacienteId, LocalDateTime fechaHora);
}
