package pe.cibertec.hospital.atenciones.service;

import pe.cibertec.hospital.atenciones.client.CitaClient;
import pe.cibertec.hospital.atenciones.client.CitaResumen;
import pe.cibertec.hospital.atenciones.dto.AtencionRequest;
import pe.cibertec.hospital.atenciones.event.EventoDominio;
import pe.cibertec.hospital.atenciones.event.EventoPublisher;
import pe.cibertec.hospital.atenciones.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.atenciones.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.atenciones.exception.ReglaNegocioException;
import pe.cibertec.hospital.atenciones.exception.TransicionInvalidaException;
import pe.cibertec.hospital.atenciones.model.Atencion;
import pe.cibertec.hospital.atenciones.model.EstadoAtencion;
import pe.cibertec.hospital.atenciones.repository.AtencionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AtencionService {
    private static final Logger log = LoggerFactory.getLogger(AtencionService.class);
    private static final String ORIGEN = "ms-atenciones";

    private final AtencionRepository repository;
    private final CitaClient citaClient;
    private final EventoPublisher publisher;
    private final Clock clock;

    public AtencionService(AtencionRepository repository, CitaClient citaClient,
                           EventoPublisher publisher, Clock clock) {
        this.repository = repository;
        this.citaClient = citaClient;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Mono<Atencion> registrar(AtencionRequest request) {
        return citaClient.obtenerCita(request.citaId())
                .flatMap(cita -> "CONFIRMADA".equals(cita.estado())
                        ? Mono.just(cita)
                        : Mono.<CitaResumen>error(new ReglaNegocioException(
                                "Solo se registran atenciones de citas CONFIRMADAS; la cita está " + cita.estado())))
                .flatMap(cita -> repository.existsByCitaId(cita.id())
                        .flatMap(existe -> Boolean.TRUE.equals(existe)
                                ? Mono.<CitaResumen>error(new RecursoDuplicadoException(
                                        "La cita " + cita.id() + " ya tiene una atención registrada"))
                                : Mono.just(cita)))
                .flatMap(cita -> repository.save(nuevaAtencion(cita, request))
                        .flatMap(guardada -> citaClient.marcarAtendida(cita.id())
                                .thenReturn(guardada)
                                .onErrorResume(ex -> {
                                    log.warn("Compensación: se elimina la atención {} porque la cita no pudo cerrarse",
                                            guardada.getCodigo());
                                    return repository.delete(guardada).then(Mono.<Atencion>error(ex));
                                })))
                .flatMap(a -> publicar("atencion.registrada", "AtencionRegistrada", a, null).thenReturn(a));
    }

    public Mono<Atencion> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException("No existe la atención con id " + id)));
    }

    public Flux<Atencion> listar() {
        return repository.findAll();
    }

    public Flux<Atencion> listarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByFechaAtencionDesc(pacienteId);
    }

    public Mono<Atencion> cambiarEstado(Long id, EstadoAtencion destino) {
        return buscarPorId(id).flatMap(a -> {
            EstadoAtencion anterior = a.getEstado();
            if (!anterior.puedeTransicionarA(destino)) {
                return Mono.<Atencion>error(new TransicionInvalidaException(
                        "La atención " + a.getCodigo() + " no puede pasar de " + anterior + " a " + destino));
            }
            a.setEstado(destino);
            a.setFechaActualizacion(LocalDateTime.now(clock));
            return repository.save(a).flatMap(g -> publicar(
                    "atencion." + destino.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                    destino == EstadoAtencion.FINALIZADA ? "AtencionFinalizada" : "AtencionAnulada",
                    g, anterior).thenReturn(g));
        });
    }

    private Atencion nuevaAtencion(CitaResumen cita, AtencionRequest request) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Atencion a = new Atencion();
        a.setCodigo("ATE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        a.setPacienteId(cita.pacienteId());
        a.setCitaId(cita.id());
        a.setArea(request.area().trim().toUpperCase(Locale.ROOT));
        a.setObservacion(request.observacion());
        a.setFechaAtencion(ahora);
        a.setFechaActualizacion(ahora);
        a.setEstado(EstadoAtencion.EN_CURSO);
        return a;
    }

    private Mono<Void> publicar(String routingKey, String tipo, Atencion a, EstadoAtencion anterior) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("pacienteId", a.getPacienteId());
        datos.put("citaId", a.getCitaId());
        datos.put("codigo", a.getCodigo());
        datos.put("area", a.getArea());
        return publisher.publicar(routingKey, EventoDominio.de(tipo, "ATENCION", String.valueOf(a.getId()),
                anterior == null ? null : anterior.name(), a.getEstado().name(), ORIGEN, datos));
    }
}
