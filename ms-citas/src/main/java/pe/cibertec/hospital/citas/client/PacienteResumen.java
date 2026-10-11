package pe.cibertec.hospital.citas.client;

public record PacienteResumen(Long id, String nombres, String apellidos, String estado) {
    public boolean estaActivo() {
        return "ACTIVO".equals(estado);
    }
}
