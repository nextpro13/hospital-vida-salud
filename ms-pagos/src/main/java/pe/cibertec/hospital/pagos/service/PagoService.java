package pe.cibertec.hospital.pagos.service;

import pe.cibertec.hospital.pagos.client.PacienteClient;
import pe.cibertec.hospital.pagos.dto.PagoRequest;
import pe.cibertec.hospital.pagos.event.EventoDominio;
import pe.cibertec.hospital.pagos.event.EventoPublisher;
import pe.cibertec.hospital.pagos.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.pagos.exception.RecursoNoEncontradoException;
import pe.cibertec.hospital.pagos.exception.ReglaNegocioException;
import pe.cibertec.hospital.pagos.exception.TransicionInvalidaException;
import pe.cibertec.hospital.pagos.model.EstadoPago;
import pe.cibertec.hospital.pagos.model.Pago;
import pe.cibertec.hospital.pagos.repository.PagoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PagoService {
    private static final String ORIGEN = "ms-pagos";

    private final PagoRepository repository;
    private final PacienteClient pacienteClient;
    private final EventoPublisher publisher;
    private final Clock clock;

    public PagoService(PagoRepository repository, PacienteClient pacienteClient,
                       EventoPublisher publisher, Clock clock) {
        this.repository = repository;
        this.pacienteClient = pacienteClient;
        this.publisher = publisher;
        this.clock = clock;
    }

    public Mono<Pago> registrar(PagoRequest request) {
        return Mono.fromRunnable(() -> validarMonto(request.monto()))
                .then(Mono.defer(() -> repository.existsByNumeroOperacion(request.numeroOperacion())))
                .flatMap(existe -> Boolean.TRUE.equals(existe)
                        ? Mono.<Boolean>error(new RecursoDuplicadoException(
                                "El número de operación " + request.numeroOperacion() + " ya fue registrado"))
                        : Mono.just(false))
                .then(Mono.defer(() -> pacienteClient.verificarExistencia(request.pacienteId())))
                .then(Mono.defer(() -> repository.save(nuevoPago(request))))
                .flatMap(p -> publicar("pago.registrado", "PagoRegistrado", p, null).thenReturn(p));
    }

    public Mono<Pago> buscarPorId(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(() -> new RecursoNoEncontradoException("No existe el pago con id " + id)));
    }

    public Flux<Pago> listar() {
        return repository.findAll();
    }

    public Flux<Pago> listarPorPaciente(Long pacienteId) {
        return repository.findByPacienteIdOrderByFechaPagoDesc(pacienteId);
    }

    public Mono<Pago> anular(Long id) {
        return buscarPorId(id).flatMap(p -> {
            if (p.getEstado() != EstadoPago.REGISTRADO) {
                return Mono.<Pago>error(new TransicionInvalidaException("El pago " + p.getNumeroOperacion() + " ya está anulado"));
            }
            p.setEstado(EstadoPago.ANULADO);
            p.setFechaActualizacion(LocalDateTime.now(clock));
            return repository.save(p)
                    .flatMap(g -> publicar("pago.anulado", "PagoAnulado", g, EstadoPago.REGISTRADO).thenReturn(g));
        });
    }

    static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto del pago debe ser mayor que cero");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("El monto admite como máximo dos decimales");
        }
    }

    private Pago nuevoPago(PagoRequest r) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Pago p = new Pago();
        p.setNumeroOperacion(r.numeroOperacion());
        p.setPacienteId(r.pacienteId());
        p.setAtencionId(r.atencionId());
        p.setConcepto(r.concepto().trim());
        p.setMonto(r.monto());
        p.setMedioPago(r.medioPago());
        p.setFechaPago(ahora);
        p.setFechaActualizacion(ahora);
        p.setEstado(EstadoPago.REGISTRADO);
        return p;
    }

    private Mono<Void> publicar(String routingKey, String tipo, Pago p, EstadoPago anterior) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("pacienteId", p.getPacienteId());
        datos.put("numeroOperacion", p.getNumeroOperacion());
        datos.put("monto", p.getMonto().toPlainString());
        datos.put("medioPago", p.getMedioPago().name());
        return publisher.publicar(routingKey, EventoDominio.de(tipo, "PAGO", String.valueOf(p.getId()),
                anterior == null ? null : anterior.name(), p.getEstado().name(), ORIGEN, datos));
    }
}
