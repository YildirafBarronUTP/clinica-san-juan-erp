package pe.clinicasanjuan.erp.comun;

/** Recurso duplicado o modificado en paralelo: HTTP 409. */
public class ConflictoException extends ReglaNegocioException {
    public ConflictoException(String codigo, String mensaje, String campo) {
        super(codigo, mensaje, campo);
    }
}
