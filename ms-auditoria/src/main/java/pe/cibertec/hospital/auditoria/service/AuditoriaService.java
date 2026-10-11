package pe.cibertec.hospital.auditoria.service;

import pe.cibertec.hospital.auditoria.event.EventoDominio;
import pe.cibertec.hospital.auditoria.model.RegistroAuditoria;
import pe.cibertec.hospital.auditoria.repository.RegistroAuditoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final RegistroAuditoriaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditoriaService(RegistroAuditoriaRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Mono<RegistroAuditoria> registrar(EventoDominio evento) {
        return repository.existsByEventoId(evento.eventoId())
                .flatMap(existe -> Boolean.TRUE.equals(existe)
                        ? Mono.<RegistroAuditoria>empty()
                        : repository.save(construir(evento)))
                .doOnNext(r -> log.info("[AUDITORIA] {} {}#{} {} -> {} (origen {})", r.getTipoEvento(),
                        r.getRecurso(), r.getRecursoId(), r.getEstadoAnterior(), r.getEstadoNuevo(), r.getOrigen()));
    }

    public Flux<RegistroAuditoria> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "fechaEvento"));
    }

    public Flux<RegistroAuditoria> historialDe(String recurso, String recursoId) {
        return repository.findByRecursoAndRecursoIdOrderByFechaEventoAsc(recurso.toUpperCase(Locale.ROOT), recursoId);
    }

    public Flux<RegistroAuditoria> porTipo(String tipoEvento) {
        return repository.findByTipoEventoOrderByFechaEventoDesc(tipoEvento);
    }

    RegistroAuditoria construir(EventoDominio e) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setEventoId(e.eventoId());
        r.setTipoEvento(e.tipo());
        r.setRecurso(e.recurso());
        r.setRecursoId(e.recursoId());
        r.setEstadoAnterior(e.estadoAnterior());
        r.setEstadoNuevo(e.estadoNuevo());
        r.setOrigen(e.origen());
        r.setFechaEvento(e.fecha() != null ? e.fecha() : LocalDateTime.now(clock));
        r.setFechaRegistro(LocalDateTime.now(clock));
        r.setDetalle(aJson(e));
        return r;
    }

    private String aJson(EventoDominio e) {
        try {
            return e.datos() == null ? "{}" : objectMapper.writeValueAsString(e.datos());
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
