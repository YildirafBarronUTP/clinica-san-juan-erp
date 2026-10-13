package pe.clinicasanjuan.erp.pacientes.service;

/** DNI peruano: exactamente 8 dígitos. */
public final class ValidadorDni {
    private ValidadorDni() {}

    public static boolean esValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }
}
