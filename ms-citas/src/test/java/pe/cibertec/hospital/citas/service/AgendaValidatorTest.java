package pe.cibertec.hospital.citas.service;

import pe.cibertec.hospital.citas.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendaValidatorTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Test
    @DisplayName("Acepta un lunes a las 09:30")
    void aceptaHorarioValido() {
        assertThatCode(() -> AgendaValidator.validar(LocalDateTime.of(2026, 10, 12, 9, 30), AHORA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza fechas pasadas")
    void rechazaFechaPasada() {
        assertThatThrownBy(() -> AgendaValidator.validar(LocalDateTime.of(2026, 10, 9, 9, 0), AHORA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("Rechaza domingos")
    void rechazaDomingo() {
        assertThatThrownBy(() -> AgendaValidator.validar(LocalDateTime.of(2026, 10, 11, 9, 0), AHORA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("domingos");
    }

    @Test
    @DisplayName("Rechaza horas fuera del horario y que no estén en bloques de 30 minutos")
    void rechazaHorarioFueraDeBloque() {
        assertThatThrownBy(() -> AgendaValidator.validar(LocalDateTime.of(2026, 10, 12, 7, 0), AHORA))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> AgendaValidator.validar(LocalDateTime.of(2026, 10, 12, 10, 15), AHORA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("30 minutos");
    }
}
