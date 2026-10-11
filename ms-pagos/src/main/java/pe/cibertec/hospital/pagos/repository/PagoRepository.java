package pe.cibertec.hospital.pagos.repository;

import pe.cibertec.hospital.pagos.model.Pago;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PagoRepository extends R2dbcRepository<Pago, Long> {
    Mono<Boolean> existsByNumeroOperacion(String numeroOperacion);

    Flux<Pago> findByPacienteIdOrderByFechaPagoDesc(Long pacienteId);
}
