package pe.cibertec.hospital.pacientes.service;

import pe.cibertec.hospital.pacientes.dto.PacienteRequest;
import pe.cibertec.hospital.pacientes.event.EventoDominio;
import pe.cibertec.hospital.pacientes.event.EventoPublisher;
import pe.cibertec.hospital.pacientes.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.pacientes.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.pacientes.exception.ReglaNegocioException;
import pe.cibertec.hospital.pacientes.model.EstadoPaciente;
import pe.cibertec.hospital.pacientes.model.Paciente;
import pe.cibertec.hospital.pacientes.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PacienteService {
    private static final String ORIGEN = "ms-pacientes";

    private final PacienteRepository repository;
    private final EventoPublisher publisher;
    private final Clock clock;

    public PacienteService(PacienteRepository repository, EventoPublisher publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Mono<Paciente> registrar(PacienteRequest request) {
        return Mono.fromRunnable(() -> validarDocumento(request.tipoDocumento(), request.numeroDocumento()))
                .then(Mono.defer(() -> repository.existsByTipoDocumentoAndNumeroDocumento(
                        request.tipoDocumento(), request.numeroDocumento())))
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Mono.<Paciente>error(new RecursoDuplicadoException(
                                "Ya existe un paciente registrado con el documento " + request.tipoDocumento()
                                        + " " + request.numeroDocumento()));
                    }
                    Paciente paciente = new Paciente();
                    aplicar(paciente, request);
                    paciente.setFechaRegistro(ahora());
                    paciente.setEstado(EstadoPaciente.ACTIVO);
                    return repository.save(paciente);
                })
                .flatMap(guardado -> publicar("paciente.registrado", "PacienteRegistrado", guardado, null)
                        .thenReturn(guardado));
    }

    public Mono<Paciente> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException("No existe el paciente con id " + id)));
    }

    public Mono<Paciente> buscarPorDocumento(String tipo, String numero) {
        return repository.findByTipoDocumentoAndNumeroDocumento(tipo, numero)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException(
                        "No existe un paciente con el documento " + tipo + " " + numero)));
    }

    public Flux<Paciente> listar() {
        return repository.findAll();
    }

    public Mono<Paciente> actualizar(Long id, PacienteRequest request) {
        return Mono.fromRunnable(() -> validarDocumento(request.tipoDocumento(), request.numeroDocumento()))
                .then(Mono.defer(() -> buscarPorId(id)))
                .flatMap(actual -> repository
                        .findByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), request.numeroDocumento())
                        .filter(otro -> !otro.getId().equals(id))
                        .flatMap(otro -> Mono.<Paciente>error(new RecursoDuplicadoException(
                                "El documento indicado ya pertenece a otro paciente")))
                        .switchIfEmpty(Mono.defer(() -> {
                            aplicar(actual, request);
                            actual.setFechaActualizacion(ahora());
                            return repository.save(actual);
                        })));
    }

    public Mono<Paciente> cambiarEstado(Long id, EstadoPaciente nuevoEstado) {
        return buscarPorId(id).flatMap(paciente -> {
            EstadoPaciente anterior = paciente.getEstado();
            if (anterior == nuevoEstado) {
                return Mono.<Paciente>error(new ReglaNegocioException("El paciente ya se encuentra " + nuevoEstado));
            }
            paciente.setEstado(nuevoEstado);
            paciente.setFechaActualizacion(ahora());
            return repository.save(paciente)
                    .flatMap(g -> publicar("paciente.estado-actualizado", "PacienteEstadoActualizado", g, anterior)
                            .thenReturn(g));
        });
    }

    static void validarDocumento(String tipo, String numero) {
        if ("DNI".equals(tipo) && (numero == null || !numero.matches("\\d{8}"))) {
            throw new ReglaNegocioException("El DNI debe tener exactamente 8 dígitos numéricos");
        }
    }

    private void aplicar(Paciente p, PacienteRequest r) {
        p.setTipoDocumento(r.tipoDocumento());
        p.setNumeroDocumento(r.numeroDocumento());
        p.setNombres(r.nombres().trim());
        p.setApellidos(r.apellidos().trim());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setTelefono(r.telefono());
        p.setCorreo(r.correo().toLowerCase());
        p.setDireccion(r.direccion());
    }

    private Mono<Void> publicar(String routingKey, String tipo, Paciente p, EstadoPaciente anterior) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("pacienteId", p.getId());
        datos.put("nombres", p.getNombres());
        EventoDominio evento = EventoDominio.de(tipo, "PACIENTE", String.valueOf(p.getId()),
                anterior == null ? null : anterior.name(), p.getEstado().name(), ORIGEN, datos);
        return publisher.publicar(routingKey, evento);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
