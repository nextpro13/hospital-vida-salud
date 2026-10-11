package pe.cibertec.hospital.citas.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoCitaTest {
    @Test
    @DisplayName("Transiciones permitidas desde PENDIENTE y CONFIRMADA")
    void transicionesPermitidas() {
        assertThat(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CONFIRMADA)).isTrue();
        assertThat(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CANCELADA)).isTrue();
        assertThat(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.ATENDIDA)).isTrue();
        assertThat(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.AUSENTE)).isTrue();
        assertThat(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.CANCELADA)).isTrue();
    }

    @Test
    @DisplayName("Una cita cancelada no se puede confirmar ni atender")
    void citaCanceladaEsFinal() {
        assertThat(EstadoCita.CANCELADA.puedeTransicionarA(EstadoCita.CONFIRMADA)).isTrue();
        assertThat(EstadoCita.CANCELADA.puedeTransicionarA(EstadoCita.ATENDIDA)).isFalse();
    }

    @Test
    @DisplayName("Una cita PENDIENTE no puede marcarse ATENDIDA sin confirmarse")
    void pendienteNoPasaDirectoAAtendida() {
        assertThat(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.ATENDIDA)).isFalse();
        assertThat(EstadoCita.ATENDIDA.puedeTransicionarA(EstadoCita.CANCELADA)).isFalse();
    }
}
