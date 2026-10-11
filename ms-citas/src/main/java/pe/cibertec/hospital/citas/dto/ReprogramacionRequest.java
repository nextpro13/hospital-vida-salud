package pe.cibertec.hospital.citas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramacionRequest(
        @NotNull(message = "es obligatoria")
        @Future(message = "debe ser una fecha futura")
        LocalDateTime nuevaFechaHora) {
}
