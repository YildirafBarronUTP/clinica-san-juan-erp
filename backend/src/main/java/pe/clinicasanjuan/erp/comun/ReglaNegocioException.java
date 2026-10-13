package pe.clinicasanjuan.erp.comun;

/**
 * Violación de una regla de negocio. El {@code codigo} es estable y es lo que
 * verifican las pruebas negativas (no el mensaje, que puede cambiar).
 */
public class ReglaNegocioException extends RuntimeException {
    private final String codigo;
    private final String campo;

    public ReglaNegocioException(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }

    public ReglaNegocioException(String codigo, String mensaje, String campo) {
        super(mensaje);
        this.codigo = codigo;
        this.campo = campo;
    }

    public String getCodigo() { return codigo; }
    public String getCampo() { return campo; }
}
