package pe.clinicasanjuan.erp.seguridad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;
import pe.clinicasanjuan.erp.seguridad.model.Rol;
import pe.clinicasanjuan.erp.seguridad.model.Usuario;
import pe.clinicasanjuan.erp.seguridad.repository.UsuarioRepository;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UsuarioService {
    /** Mínimo 8 caracteres con al menos una letra y un dígito. */
    static final Pattern POLITICA_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$");

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.repo = repo;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repo.findAll(org.springframework.data.domain.Sort.by("username"));
    }

    @Transactional
    public Usuario crear(String username, String password, String nombreCompleto, Rol rol) {
        if (!POLITICA_PASSWORD.matcher(password).matches()) {
            throw new ReglaNegocioException("PASSWORD_DEBIL",
                    "La contraseña debe tener al menos 8 caracteres, una letra y un número", "password");
        }
        if (repo.existsByUsernameIgnoreCase(username)) {
            throw new ConflictoException("USUARIO_DUPLICADO", "El nombre de usuario ya existe", "username");
        }
        Usuario u = repo.save(new Usuario(username.toLowerCase(), encoder.encode(password), nombreCompleto, rol));
        auditoria.registrar("CREAR", "Usuario", u.getId(), "rol=" + rol);
        return u;
    }

    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo, String solicitante) {
        Usuario u = repo.findById(id).orElseThrow(() -> new NoEncontradoException("Usuario", id));
        if (!activo && u.getUsername().equalsIgnoreCase(solicitante)) {
            throw new ReglaNegocioException("AUTO_DESACTIVACION", "Un administrador no puede desactivarse a sí mismo");
        }
        u.setActivo(activo);
        auditoria.registrar(activo ? "ACTIVAR" : "DESACTIVAR", "Usuario", u.getId(), null);
        return u;
    }
}
