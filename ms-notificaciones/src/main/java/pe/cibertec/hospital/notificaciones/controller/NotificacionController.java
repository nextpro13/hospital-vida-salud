package pe.cibertec.hospital.notificaciones.controller;

import pe.cibertec.hospital.notificaciones.model.Notificacion;
import pe.cibertec.hospital.notificaciones.service.NotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Notificacion> listar() {
        return service.listar();
    }

    @GetMapping("/paciente/{pacienteId}")
    public Flux<Notificacion> listarPorPaciente(@PathVariable("pacienteId") Long pacienteId) {
        return service.listarPorPaciente(pacienteId);
    }
}
