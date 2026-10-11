package pe.cibertec.hospital.atenciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtencionRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser positivo")
        Long citaId,

        @NotBlank(message = "es obligatoria")
        @Size(max = 60, message = "admite como máximo 60 caracteres")
        String area,

        @Size(max = 250, message = "admite como máximo 250 caracteres")
        String observacion) {
}
