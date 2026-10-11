package pe.cibertec.hospital.pagos.controller;

import pe.cibertec.hospital.pagos.dto.PagoRequest;
import pe.cibertec.hospital.pagos.model.Pago;
import pe.cibertec.hospital.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Pago> registrar(@Valid @RequestBody PagoRequest request) {
        return service.registrar(request);
    }

    @GetMapping
    public Flux<Pago> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Mono<Pago> obtener(@PathVariable("id") Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/paciente/{pacienteId}")
    public Flux<Pago> listarPorPaciente(@PathVariable("pacienteId") Long pacienteId) {
        return service.listarPorPaciente(pacienteId);
    }

    @PatchMapping("/{id}/anular")
    public Mono<Pago> anular(@PathVariable("id") Long id) {
        return service.anular(id);
    }
}
