package pe.cibertec.hospital.pacientes.controller;

import pe.cibertec.hospital.pacientes.dto.CambioEstadoRequest;
import pe.cibertec.hospital.pacientes.dto.PacienteRequest;
import pe.cibertec.hospital.pacientes.model.Paciente;
import pe.cibertec.hospital.pacientes.service.PacienteService;
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
@RequestMapping("/api/pacientes")
public class PacienteController {
    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Paciente> registrar(@Valid @RequestBody PacienteRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<Paciente> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Mono<Paciente> obtener(@PathVariable("id") Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/documento")
    public Mono<Paciente> obtenerPorDocumento(@RequestParam("tipo") String tipo,
                                              @RequestParam("numero") String numero) {
        return service.buscarPorDocumento(tipo, numero);
    }

    @PutMapping("/{id}")
    public Mono<Paciente> actualizar(@PathVariable("id") Long id, @Valid @RequestBody PacienteRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Paciente> cambiarEstado(@PathVariable("id") Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
