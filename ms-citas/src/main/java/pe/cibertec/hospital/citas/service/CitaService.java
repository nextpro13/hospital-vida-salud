package pe.cibertec.hospital.citas.service;

import pe.cibertec.hospital.citas.client.PacienteClient;
import pe.cibertec.hospital.citas.client.PacienteResumen;
import pe.cibertec.hospital.citas.dto.CitaRequest;
import pe.cibertec.hospital.citas.event.EventoDominio;
import pe.cibertec.hospital.citas.event.EventoPublisher;
import pe.cibertec.hospital.citas.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.citas.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.citas.exception.ReglaNegocioException;
import pe.cibertec.hospital.citas.exception.TransicionInvalidaException;
import pe.cibertec.hospital.citas.model.Cita;
import pe.cibertec.hospital.citas.model.EstadoCita;
import pe.cibertec.hospital.citas.repository.CitaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CitaService {
    private static final String ORIGEN = "ms-citas";

    private final CitaRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublisher publisher;
    private final Clock clock;

    public CitaService(CitaRepository repository, PacienteClient pacienteClient,
                       EventoPublisher publisher, Clock clock) {
        this.repository = repository;
        this.pacienteClient = pacienteClient;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Mono<Cita> registrar(CitaRequest request) {
        return Mono.fromRunnable(() -> AgendaValidator.validar(request.fechaHora(), ahora()))
                .then(Mono.defer(() -> pacienteClient.obtenerPaciente(request.pacienteId())))
                .flatMap(paciente -> paciente.estaActivo()
                        ? Mono.just(paciente)
                        : Mono.<PacienteResumen>error(new ReglaNegocioException(
                                "El paciente " + request.pacienteId() + " está INACTIVO y no puede reservar citas")))
                .then(Mono.defer(() -> verificarDisponibilidad(request.pacienteId(), request.profesionalCodigo(),
                        request.fechaHora(), null)))
                .then(Mono.defer(() -> repository.save(nuevaCita(request))))
                .flatMap(cita -> publicarEvento("cita.creada", "CitaCreada", cita, null, Map.of()).thenReturn(cita));
    }

    public Mono<Cita> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException("No existe la cita con id " + id)));
    }

    public Mono<Cita> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException("No existe la cita con código " + codigo)));
    }

    public Flux<Cita> listar(EstadoCita estado) {
        return estado == null ? repository.findAll() : repository.findByEstadoOrderByFechaHoraAsc(estado);
    }

    public Flux<Cita> listarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByFechaHoraAsc(pacienteId);
    }

    public Mono<Cita> cambiarEstado(Long id, EstadoCita destino) {
        return buscarPorId(id).flatMap(cita -> {
            EstadoCita anterior = cita.getEstado();
            if (!anterior.puedeTransicionarA(destino)) {
                return Mono.<Cita>error(new TransicionInvalidaException(
                        "La cita " + cita.getCodigo() + " no puede pasar de " + anterior + " a " + destino));
            }
            if (destino == EstadoCita.CONFIRMADA && !cita.getFechaHora().isAfter(ahora())) {
                return Mono.<Cita>error(new ReglaNegocioException("No se puede confirmar una cita cuya fecha ya pasó"));
            }
            cita.setEstado(destino);
            cita.setFechaActualizacion(ahora());
            return repository.save(cita)
                    .flatMap(guardada -> publicarEvento("cita." + destino.name().toLowerCase(Locale.ROOT),
                            nombreEvento(destino), guardada, anterior, Map.of()).thenReturn(guardada));
        });
    }

    public Mono<Cita> reprogramar(Long id, LocalDateTime nuevaFechaHora) {
        return buscarPorId(id).flatMap(cita -> {
            if (!cita.getEstado().esActiva()) {
                return Mono.<Cita>error(new TransicionInvalidaException(
                        "Solo se reprograman citas PENDIENTE o CONFIRMADA; la cita está " + cita.getEstado()));
            }
            EstadoCita anterior = cita.getEstado();
            LocalDateTime fechaAnterior = cita.getFechaHora();
            return Mono.fromRunnable(() -> AgendaValidator.validar(nuevaFechaHora, ahora()))
                    .then(Mono.defer(() -> verificarDisponibilidad(cita.getPacienteId(), cita.getProfesionalCodigo(),
                            nuevaFechaHora, cita.getId())))
                    .then(Mono.defer(() -> {
                        cita.setFechaHora(nuevaFechaHora);
                        cita.setEstado(EstadoCita.PENDIENTE);
                        cita.setFechaActualizacion(ahora());
                        return repository.save(cita);
                    }))
                    .flatMap(guardada -> publicarEvento("cita.reprogramada", "CitaReprogramada", guardada, anterior,
                            Map.of("fechaAnterior", fechaAnterior.toString())).thenReturn(guardada));
        });
    }

    private Mono<Void> verificarDisponibilidad(Long pacienteId, String profesional, LocalDateTime fechaHora, Long excluirId) {
        Mono<Boolean> profesionalOcupado = repository.findByProfesionalCodigoAndFechaHora(profesional, fechaHora)
                .filter(c -> c.getEstado().esActiva() && !c.getId().equals(excluirId))
                .hasElements();
        Mono<Boolean> pacienteOcupado = repository.findByPacienteIdAndFechaHora(pacienteId, fechaHora)
                .filter(c -> c.getEstado().esActiva() && !c.getId().equals(excluirId))
                .hasElements();
        return Mono.zip(profesionalOcupado, pacienteOcupado).flatMap(ocupado -> {
            if (ocupado.getT1()) {
                return Mono.<Void>error(new RecursoDuplicadoException(
                        "El profesional " + profesional + " ya tiene una cita activa el " + fechaHora));
            }
            if (ocupado.getT2()) {
                return Mono.<Void>error(new RecursoDuplicadoException(
                        "El paciente ya tiene otra cita activa en el mismo horario"));
            }
            return Mono.<Void>empty();
        });
    }

    private Cita nuevaCita(CitaRequest request) {
        LocalDateTime ahora = ahora();
        Cita cita = new Cita();
        cita.setCodigo("CIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        cita.setPacienteId(request.pacienteId());
        cita.setEspecialidad(request.especialidad().trim().toUpperCase(Locale.ROOT));
        cita.setFechaHora(request.fechaHora());
        cita.setProfesionalCodigo(request.profesionalCodigo());
        cita.setMotivo(request.motivo());
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);
        return cita;
    }

    static String nombreEvento(EstadoCita estado) {
        return switch (estado) {
            case PENDIENTE -> "CitaPendiente";
            case CONFIRMADA -> "CitaConfirmada";
            case ATENDIDA -> "CitaAtendida";
            case CANCELADA -> "CitaCancelada";
            case AUSENTE -> "CitaAusente";
        };
    }

    private Mono<Void> publicarEvento(String routingKey, String tipo, Cita cita, EstadoCita anterior,
                                      Map<String, Object> extra) {
        Map<String, Object> datos = new HashMap<>(extra);
        datos.put("pacienteId", cita.getPacienteId());
        datos.put("codigo", cita.getCodigo());
        datos.put("especialidad", cita.getEspecialidad());
        datos.put("fechaHora", cita.getFechaHora().toString());
        datos.put("profesionalCodigo", cita.getProfesionalCodigo());
        EventoDominio evento = EventoDominio.de(tipo, "CITA", String.valueOf(cita.getId()),
                anterior == null ? null : anterior.name(), cita.getEstado().name(), ORIGEN, datos);
        return publisher.publicar(routingKey, evento);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
