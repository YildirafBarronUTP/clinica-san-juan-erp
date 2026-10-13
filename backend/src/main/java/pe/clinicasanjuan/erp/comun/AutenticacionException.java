package pe.clinicasanjuan.erp.comun;

/** Fallo de autenticación: HTTP 401. */
public class AutenticacionException extends ReglaNegocioException {
    public AutenticacionException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
