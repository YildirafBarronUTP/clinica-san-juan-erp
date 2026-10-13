package pe.clinicasanjuan.erp.comun;

public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String entidad, Object id) {
        super(entidad + " " + id + " no existe");
    }
}
