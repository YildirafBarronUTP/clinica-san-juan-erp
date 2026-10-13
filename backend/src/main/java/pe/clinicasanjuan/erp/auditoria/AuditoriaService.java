package pe.clinicasanjuan.erp.auditoria;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Bitácora: toda operación de escritura registra quién, qué y cuándo. */
@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    public void registrar(String accion, String entidad, Object entidadId, String detalle) {
        registrarComo(usuarioActual(), accion, entidad, entidadId, detalle);
    }

    public void registrarComo(String usuario, String accion, String entidad, Object entidadId, String detalle) {
        repo.save(new RegistroAuditoria(usuario, accion, entidad,
                entidadId == null ? null : entidadId.toString(), detalle, reloj.instant()));
    }

    public static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "sistema" : a.getName();
    }
}
