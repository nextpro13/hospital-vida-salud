package pe.cibertec.hospital.auditoria.controller;

import pe.cibertec.hospital.auditoria.model.RegistroAuditoria;
import pe.cibertec.hospital.auditoria.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<RegistroAuditoria> listar() {
        return service.listar();
    }

    @GetMapping("/recurso/{recurso}/{recursoId}")
    public Flux<RegistroAuditoria> historial(@PathVariable("recurso") String recurso,
                                             @PathVariable("recursoId") String recursoId) {
        return service.historialDe(recurso, recursoId);
    }

    @GetMapping("/tipo/{tipoEvento}")
    public Flux<RegistroAuditoria> porTipo(@PathVariable("tipoEvento") String tipoEvento) {
        return service.porTipo(tipoEvento);
    }
}
