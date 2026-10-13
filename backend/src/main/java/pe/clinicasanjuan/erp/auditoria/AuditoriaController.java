package pe.clinicasanjuan.erp.auditoria;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.clinicasanjuan.erp.comun.Pagina;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final AuditoriaRepository repo;

    public AuditoriaController(AuditoriaRepository repo) {
        this.repo = repo;
    }

    public record AuditoriaDto(Long id, String usuario, String accion, String entidad, String entidadId,
                               String detalle, Instant fecha) {
        static AuditoriaDto de(RegistroAuditoria a) {
            return new AuditoriaDto(a.getId(), a.getUsuario(), a.getAccion(), a.getEntidad(), a.getEntidadId(),
                    a.getDetalle(), a.getFecha());
        }
    }

    @GetMapping
    public Pagina<AuditoriaDto> buscar(@RequestParam(required = false) String entidad,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "'hasta' no puede ser anterior a 'desde'", "hasta");
        }
        Instant ini = desde.atStartOfDay(LIMA).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(LIMA).toInstant();
        return Pagina.de(repo.buscar(entidad, ini, fin, PageRequest.of(page, Math.min(size, 100))), AuditoriaDto::de);
    }
}
