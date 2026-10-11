package pe.cibertec.hospital.pacientes.repository;

import pe.cibertec.hospital.pacientes.model.Paciente;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface PacienteRepository extends R2dbcRepository<Paciente, Long> {
    Mono<Boolean> existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    Mono<Paciente> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
