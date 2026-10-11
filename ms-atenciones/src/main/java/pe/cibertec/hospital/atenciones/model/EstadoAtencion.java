package pe.cibertec.hospital.atenciones.model;

public enum EstadoAtencion {
    EN_CURSO,
    FINALIZADA,
    ANULADA;

    public boolean puedeTransicionarA(EstadoAtencion destino) {
        return this == EN_CURSO && (destino == FINALIZADA || destino == ANULADA);
    }
}
