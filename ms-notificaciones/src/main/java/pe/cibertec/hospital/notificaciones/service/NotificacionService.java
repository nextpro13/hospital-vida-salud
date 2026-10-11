package pe.cibertec.hospital.notificaciones.service;

import pe.cibertec.hospital.notificaciones.event.EventoDominio;
import pe.cibertec.hospital.notificaciones.model.Notificacion;
import pe.cibertec.hospital.notificaciones.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository repository;
    private final Clock clock;

    public NotificacionService(NotificacionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Mono<Notificacion> procesar(EventoDominio evento) {
        return repository.existsByEventoId(evento.eventoId())
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        log.info("Evento {} ya notificado; se ignora el duplicado", evento.eventoId());
                        return Mono.<Notificacion>empty();
                    }
                    return repository.save(construir(evento));
                })
                .doOnNext(n -> log.info("[NOTIFICACION SIMULADA] paciente={} evento={} canal={} -> {}",
                        n.getPacienteId(), n.getTipoEvento(), n.getCanal(), n.getMensaje()));
    }

    public Flux<Notificacion> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    public Flux<Notificacion> listarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByFechaCreacionDesc(pacienteId);
    }

    Notificacion construir(EventoDominio e) {
        Notificacion n = new Notificacion();
        n.setEventoId(e.eventoId());
        n.setTipoEvento(e.tipo());
        n.setPacienteId(numero(e.datos(), "pacienteId"));
        n.setCanal("EMAIL_SIMULADO");
        n.setMensaje(mensaje(e));
        n.setEstado("ENVIADA_SIMULADA");
        n.setFechaCreacion(LocalDateTime.now(clock));
        return n;
    }

    static String mensaje(EventoDominio e) {
        Map<String, Object> d = e.datos() == null ? Map.of() : e.datos();
        String codigo = texto(d, "codigo");
        String fecha = texto(d, "fechaHora").replace('T', ' ');
        String especialidad = texto(d, "especialidad");
        return switch (e.tipo()) {
            case "PacienteRegistrado" -> "Bienvenido(a) " + texto(d, "nombres") + ": su registro en el Hospital Vida Salud fue exitoso.";
            case "CitaCreada" -> "Su cita " + codigo + " de " + especialidad + " fue registrada para el " + fecha + ". Queda pendiente de confirmación.";
            case "CitaConfirmada" -> "Su cita " + codigo + " de " + especialidad + " para el " + fecha + " ha sido CONFIRMADA.";
            case "CitaCancelada" -> "Su cita " + codigo + " del " + fecha + " ha sido CANCELADA.";
            case "CitaReprogramada" -> "Su cita " + codigo + " fue reprogramada para el " + fecha + ". Debe confirmarse nuevamente.";
            case "CitaAusente" -> "Registramos su inasistencia a la cita " + codigo + ". Puede solicitar una nueva cita.";
            case "CitaAtendida", "AtencionRegistrada" -> "Su atención (" + codigo + ") fue registrada. Gracias por su visita.";
            case "PagoRegistrado" -> "Se registró su pago de S/ " + texto(d, "monto") + " (operación " + texto(d, "numeroOperacion") + ").";
            default -> "Actualización de " + e.recurso() + " " + e.recursoId() + ": " + e.estadoNuevo();
        };
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        return valor == null ? "" : String.valueOf(valor);
    }

    private static Long numero(Map<String, Object> datos, String clave) {
        if (datos == null || datos.get(clave) == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(datos.get(clave)));
    }
}
