package pe.cibertec.hospital.citas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser positivo")
        Long pacienteId,

        @NotBlank(message = "es obligatoria")
        @Size(max = 60, message = "admite como máximo 60 caracteres")
        String especialidad,

        @NotNull(message = "es obligatoria")
        @Future(message = "debe ser una fecha futura")
        LocalDateTime fechaHora,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "debe tener el formato AAA-999 (ej. MED-001)")
        String profesionalCodigo,

        @Size(max = 200, message = "admite como máximo 200 caracteres")
        String motivo) {
}
