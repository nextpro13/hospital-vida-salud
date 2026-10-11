package pe.cibertec.hospital.auditoria.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record EventoDominio(
        String eventoId,
        String tipo,
        String recurso,
        String recursoId,
        String estadoAnterior,
        String estadoNuevo,
        String origen,
        LocalDateTime fecha,
        Map<String, Object> datos) {
    public static EventoDominio de(String tipo, String recurso, String recursoId, String estadoAnterior,
                                   String estadoNuevo, String origen, Map<String, Object> datos) {
        return new EventoDominio(UUID.randomUUID().toString(), tipo, recurso, recursoId, estadoAnterior,
                estadoNuevo, origen, LocalDateTime.now(), datos);
    }
}
