package pe.cibertec.hospital.pacientes.dto;

import pe.cibertec.hospital.pacientes.model.EstadoPaciente;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull(message = "es obligatorio") EstadoPaciente estado) {
}
