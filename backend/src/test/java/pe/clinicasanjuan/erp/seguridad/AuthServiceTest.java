package pe.clinicasanjuan.erp.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.AutenticacionException;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.fixtures.Relojes;
import pe.clinicasanjuan.erp.seguridad.model.Rol;
import pe.clinicasanjuan.erp.seguridad.model.Usuario;
import pe.clinicasanjuan.erp.seguridad.repository.UsuarioRepository;
import pe.clinicasanjuan.erp.seguridad.service.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock UsuarioRepository repo;
    @Mock AuditoriaService auditoria;
    AuthService auth;
    Usuario medico;

    @BeforeEach
    void setUp() {
        var reloj = Relojes.fijo();
        auth = new AuthService(repo, ENCODER, new JwtService(new JwtProperties(JwtServiceTest.SECRETO, 60), reloj),
                new SeguridadProperties(5, 15), auditoria, reloj);
        medico = new Usuario("medico", ENCODER.encode("Clinica2026"), "Dr. Médico", Rol.MEDICO);
        org.mockito.Mockito.lenient().when(repo.findByUsernameIgnoreCase("medico")).thenReturn(Optional.of(medico));
    }

    @Test
    @DisplayName("CP-SEG-010 Login correcto devuelve token y rol")
    void loginOk() {
        var r = auth.login("medico", "Clinica2026");
        assertThat(r.rol()).isEqualTo("MEDICO");
        assertThat(r.token()).isNotBlank();
    }

    @Test
    @DisplayName("CP-SEG-011 Valor límite: 4 fallos no bloquean, el 5.º bloquea aunque luego la clave sea correcta")
    void bloqueoAlQuintoIntento() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> auth.login("medico", "mala")).extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        }
        assertThat(medico.estaBloqueado(Relojes.fijo().instant())).isFalse();

        assertThatThrownBy(() -> auth.login("medico", "mala")).isInstanceOf(AutenticacionException.class);
        assertThatThrownBy(() -> auth.login("medico", "Clinica2026")).extracting("codigo").isEqualTo("USUARIO_BLOQUEADO");
    }

    @Test
    @DisplayName("CP-SEG-012 El bloqueo expira a los 15 minutos")
    void bloqueoExpira() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> auth.login("medico", "mala"));
        }
        var despues = java.time.Clock.offset(Relojes.fijo(), java.time.Duration.ofMinutes(15));
        AuthService authLuego = new AuthService(repo, ENCODER,
                new JwtService(new JwtProperties(JwtServiceTest.SECRETO, 60), despues),
                new SeguridadProperties(5, 15), auditoria, despues);
        assertThat(authLuego.login("medico", "Clinica2026").username()).isEqualTo("medico");
    }

    @Test
    @DisplayName("CP-SEG-013 Usuario inexistente o inactivo no entra")
    void inexistenteEInactivo() {
        when(repo.findByUsernameIgnoreCase("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login("nadie", "x")).extracting("codigo").isEqualTo("CREDENCIALES_INVALIDAS");
        medico.setActivo(false);
        assertThatThrownBy(() -> auth.login("medico", "Clinica2026")).extracting("codigo").isEqualTo("USUARIO_INACTIVO");
    }

    @Test
    @DisplayName("CP-SEG-014 Política de contraseña, duplicados y auto-desactivación de ADMIN")
    void usuarios() {
        UsuarioService us = new UsuarioService(repo, ENCODER, auditoria);
        assertThatThrownBy(() -> us.crear("nuevo", "corta1", "N", Rol.CAJA)).extracting("codigo").isEqualTo("PASSWORD_DEBIL");
        assertThatThrownBy(() -> us.crear("nuevo", "sololetras", "N", Rol.CAJA)).extracting("codigo").isEqualTo("PASSWORD_DEBIL");

        when(repo.existsByUsernameIgnoreCase("medico")).thenReturn(true);
        assertThatThrownBy(() -> us.crear("medico", "Clave1234", "N", Rol.CAJA)).isInstanceOf(ConflictoException.class);

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(us.crear("Nuevo.User", "Clave1234", "N", Rol.CAJA).getUsername()).isEqualTo("nuevo.user");

        Usuario admin = new Usuario("admin", "x", "Admin", Rol.ADMIN);
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> us.cambiarEstado(1L, false, "admin")).extracting("codigo").isEqualTo("AUTO_DESACTIVACION");
        assertThat(us.cambiarEstado(1L, false, "otro.admin").isActivo()).isFalse();

        when(repo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(admin));
        assertThat(us.listar()).hasSize(1);
    }
}
