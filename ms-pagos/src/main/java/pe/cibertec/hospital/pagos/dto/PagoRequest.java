package pe.cibertec.hospital.pagos.dto;

import pe.cibertec.hospital.pagos.model.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagoRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser positivo")
        Long pacienteId,

        @Positive(message = "debe ser positivo")
        Long atencionId,

        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite como máximo 120 caracteres")
        String concepto,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.01", message = "debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "admite hasta 8 enteros y 2 decimales")
        BigDecimal monto,

        @NotNull(message = "es obligatorio")
        MedioPago medioPago,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "[A-Z0-9-]{6,30}", message = "debe tener entre 6 y 30 caracteres (A-Z, 0-9, guion)")
        String numeroOperacion) {
}
