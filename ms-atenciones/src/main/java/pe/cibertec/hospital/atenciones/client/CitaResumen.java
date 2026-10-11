package pe.cibertec.hospital.atenciones.client;

import java.time.LocalDateTime;

public record CitaResumen(Long id, String codigo, Long pacienteId, String estado, LocalDateTime fechaHora) {
}
