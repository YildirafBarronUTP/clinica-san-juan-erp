package pe.clinicasanjuan.erp.seed;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Solo existe con el perfil Spring "test" (E2E y CI). Nunca se activa en staging.
 * Restaura la base al estado de la semilla dev para que cada spec E2E parta igual.
 */
@RestController
@Profile("test")
@RequestMapping("/api/v1/test-support")
public class SoportePruebasController {
    private final JdbcTemplate jdbc;
    private final CargadorSemilla semilla;

    public SoportePruebasController(JdbcTemplate jdbc, CargadorSemilla semilla) {
        this.jdbc = jdbc;
        this.semilla = semilla;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        jdbc.execute("""
                truncate table movimiento_kardex, lote, producto, alergia, paciente, auditoria, usuario
                restart identity cascade""");
        jdbc.execute("select setval('historia_clinica_seq', 1, false)");
        semilla.cargar("dev");
    }
}
