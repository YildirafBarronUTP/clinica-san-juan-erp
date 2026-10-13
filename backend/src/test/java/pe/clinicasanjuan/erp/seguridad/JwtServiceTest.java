package pe.clinicasanjuan.erp.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.clinicasanjuan.erp.fixtures.Relojes;
import pe.clinicasanjuan.erp.seguridad.model.Rol;
import pe.clinicasanjuan.erp.seguridad.service.JwtProperties;
import pe.clinicasanjuan.erp.seguridad.service.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres!!";

    @Test
    @DisplayName("CP-SEG-001 Un token emitido se valida y conserva usuario y rol")
    void emiteYValida() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, 120), Relojes.fijo());
        String token = jwt.emitir("farmacia", Rol.FARMACIA).token();
        assertThat(jwt.validar(token)).contains(new JwtService.Sesion("farmacia", Rol.FARMACIA));
    }

    @Test
    @DisplayName("CP-SEG-002 Un token vencido se rechaza")
    void vencido() {
        String token = new JwtService(new JwtProperties(SECRETO, 120), Relojes.fijo()).emitir("admin", Rol.ADMIN).token();
        JwtService mañana = new JwtService(new JwtProperties(SECRETO, 120), Relojes.fijoEn(Relojes.HOY.plusDays(1)));
        assertThat(mañana.validar(token)).isEmpty();
    }

    @Test
    @DisplayName("CP-SEG-003 Un token manipulado o firmado con otra clave se rechaza")
    void manipulado() {
        JwtService jwt = new JwtService(new JwtProperties(SECRETO, 120), Relojes.fijo());
        String token = jwt.emitir("caja", Rol.CAJA).token();
        String[] partes = token.split("\\.");
        // Cambia el payload (intento de escalar a ADMIN) sin volver a firmar
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"caja\",\"rol\":\"ADMIN\"}".getBytes());
        assertThat(jwt.validar(partes[0] + "." + payloadFalso + "." + partes[2])).isEmpty();

        JwtService otraClave = new JwtService(new JwtProperties(SECRETO + "x", 120), Relojes.fijo());
        assertThat(otraClave.validar(token)).isEmpty();
        assertThat(jwt.validar("basura")).isEmpty();
    }

    @Test
    @DisplayName("CP-SEG-004 Un secreto corto impide arrancar")
    void secretoCorto() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("corto", 120), Relojes.fijo()))
                .isInstanceOf(IllegalStateException.class);
    }
}
