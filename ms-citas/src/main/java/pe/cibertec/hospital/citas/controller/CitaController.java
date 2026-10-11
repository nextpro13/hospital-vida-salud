package pe.cibertec.hospital.citas.controller;

import pe.cibertec.hospital.citas.dto.CitaRequest;
import pe.cibertec.hospital.citas.dto.ReprogramacionRequest;
import pe.cibertec.hospital.citas.model.Cita;
import pe.cibertec.hospital.citas.model.EstadoCita;
import pe.cibertec.hospital.citas.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Cita> registrar(@Valid @RequestBody CitaRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<Cita> listar(@RequestParam(name = "estado", required = false) EstadoCita estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public Mono<Cita> obtener(@PathVariable("id") Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/codigo/{codigo}")
    public Mono<Cita> obtenerPorCodigo(@PathVariable("codigo") String codigo) {
        return service.buscarPorCodigo(codigo);
    }

    @GetMapping("/paciente/{pacienteId}")
    public Flux<Cita> listarPorPaciente(@PathVariable("pacienteId") Long pacienteId) {
        return service.listarPorPaciente(pacienteId);
    }

    @PatchMapping("/{id}/confirmar")
    public Mono<Cita> confirmar(@PathVariable("id") Long id) {
        return service.cambiarEstado(id, EstadoCita.CONFIRMADA);
    }

    @PatchMapping("/{id}/cancelar")
    public Mono<Cita> cancelar(@PathVariable("id") Long id) {
        return service.cambiarEstado(id, EstadoCita.CANCELADA);
    }

    @PatchMapping("/{id}/atender")
    public Mono<Cita> atender(@PathVariable("id") Long id) {
        return service.cambiarEstado(id, EstadoCita.ATENDIDA);
    }

    @PatchMapping("/{id}/ausente")
    public Mono<Cita> marcarAusente(@PathVariable("id") Long id) {
        return service.cambiarEstado(id, EstadoCita.AUSENTE);
    }

    @PutMapping("/{id}/reprogramar")
    public Mono<Cita> reprogramar(@PathVariable("id") Long id, @Valid @RequestBody ReprogramacionRequest request) {
        return service.reprogramar(id, request.nuevaFechaHora());
    }
}
