package pe.cibertec.hospital.citas.model;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    AUSENTE;

    public boolean puedeTransicionarA(EstadoCita destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == ATENDIDA || destino == CANCELADA || destino == AUSENTE;
            case ATENDIDA, CANCELADA, AUSENTE -> false;
        };
    }

    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
