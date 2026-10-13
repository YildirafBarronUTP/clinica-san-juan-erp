package pe.clinicasanjuan.erp.seguridad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.AutenticacionException;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;
import pe.clinicasanjuan.erp.seguridad.model.Usuario;
import pe.clinicasanjuan.erp.seguridad.repository.UsuarioRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SeguridadProperties props;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt,
                       SeguridadProperties props, AuditoriaService auditoria, Clock reloj) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    /**
     * Autentica y emite un JWT. Los fallos se confirman aunque la operación lance
     * excepción (noRollbackFor), para que el contador de intentos persista.
     */
    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public ResultadoLogin login(String username, String password) {
        Instant ahora = reloj.instant();
        Usuario u = usuarios.findByUsernameIgnoreCase(username)
                .orElseThrow(AuthService::credencialesInvalidas);

        if (!u.isActivo()) {
            throw new AutenticacionException("USUARIO_INACTIVO", "El usuario está desactivado");
        }
        if (u.estaBloqueado(ahora)) {
            throw new AutenticacionException("USUARIO_BLOQUEADO",
                    "Usuario bloqueado por intentos fallidos; intente más tarde");
        }
        if (!encoder.matches(password, u.getPasswordHash())) {
            u.registrarFallo(props.maxIntentosLogin(), ahora.plus(Duration.ofMinutes(props.minutosBloqueo())));
            auditoria.registrarComo(u.getUsername(), "LOGIN_FALLIDO", "Usuario", u.getId(), null);
            throw credencialesInvalidas();
        }
        u.registrarExito();
        auditoria.registrarComo(u.getUsername(), "LOGIN", "Usuario", u.getId(), null);
        JwtService.TokenEmitido t = jwt.emitir(u.getUsername(), u.getRol());
        return new ResultadoLogin(t.token(), t.expiraEn(), u.getUsername(), u.getNombreCompleto(), u.getRol().name());
    }

    private static AutenticacionException credencialesInvalidas() {
        return new AutenticacionException("CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos");
    }

    public record ResultadoLogin(String token, Instant expiraEn, String username, String nombreCompleto, String rol) {}
}
