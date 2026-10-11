package pe.cibertec.hospital.pacientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "DNI|CE|PASAPORTE", message = "debe ser DNI, CE o PASAPORTE")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "debe tener entre 8 y 12 caracteres alfanuméricos")
        String numeroDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "admite como máximo 100 caracteres")
        String nombres,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "admite como máximo 100 caracteres")
        String apellidos,

        @NotNull(message = "es obligatoria")
        @Past(message = "debe ser una fecha pasada")
        LocalDate fechaNacimiento,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "\\+?[0-9]{7,15}", message = "debe contener entre 7 y 15 dígitos")
        String telefono,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato válido")
        @Size(max = 120, message = "admite como máximo 120 caracteres")
        String correo,

        @Size(max = 200, message = "admite como máximo 200 caracteres")
        String direccion) {
}
