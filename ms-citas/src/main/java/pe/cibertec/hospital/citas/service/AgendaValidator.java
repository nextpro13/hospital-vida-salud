package pe.cibertec.hospital.citas.service;

import pe.cibertec.hospital.citas.exception.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AgendaValidator {
    static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    static final LocalTime ULTIMA_CITA = LocalTime.of(19, 30);
    static final int DIAS_MAXIMOS_ANTICIPACION = 90;

    private AgendaValidator() {
    }

    public static void validar(LocalDateTime fechaHora, LocalDateTime ahora) {
        if (fechaHora == null) {
            throw new ReglaNegocioException("La fecha y hora de la cita es obligatoria");
        }
        if (!fechaHora.isAfter(ahora)) {
            throw new ReglaNegocioException("La cita debe programarse en una fecha y hora futura");
        }
        if (fechaHora.isAfter(ahora.plusDays(DIAS_MAXIMOS_ANTICIPACION))) {
            throw new ReglaNegocioException("Solo se permiten citas con hasta " + DIAS_MAXIMOS_ANTICIPACION + " días de anticipación");
        }
        if (fechaHora.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReglaNegocioException("No se programan citas los domingos");
        }
        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(HORA_INICIO) || hora.isAfter(ULTIMA_CITA)) {
            throw new ReglaNegocioException("El horario de citas es de 08:00 a 19:30");
        }
        if (hora.getMinute() % 30 != 0 || hora.getSecond() != 0 || hora.getNano() != 0) {
            throw new ReglaNegocioException("Las citas se programan en bloques de 30 minutos (hh:00 u hh:30)");
        }
    }
}
