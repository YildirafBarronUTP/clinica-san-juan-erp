package pe.clinicasanjuan.erp.comun;

import java.time.Instant;
import java.util.List;

/** Formato único de error de la API. */
public record ApiError(int status, String codigo, String mensaje, List<CampoError> campos, Instant fecha) {
    public record CampoError(String campo, String mensaje) {}

    public static ApiError of(int status, String codigo, String mensaje) {
        return new ApiError(status, codigo, mensaje, List.of(), Instant.now());
    }

    public static ApiError of(int status, String codigo, String mensaje, String campo) {
        List<CampoError> campos = campo == null ? List.of() : List.of(new CampoError(campo, mensaje));
        return new ApiError(status, codigo, mensaje, campos, Instant.now());
    }
}
