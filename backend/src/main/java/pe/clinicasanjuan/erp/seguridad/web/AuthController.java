package pe.clinicasanjuan.erp.seguridad.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.seguridad.repository.UsuarioRepository;
import pe.clinicasanjuan.erp.seguridad.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final UsuarioRepository usuarios;

    public AuthController(AuthService auth, UsuarioRepository usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record Yo(String username, String nombreCompleto, String rol) {}

    @PostMapping("/login")
    public AuthService.ResultadoLogin login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.username().trim(), req.password());
    }

    @GetMapping("/me")
    public Yo yo(Authentication a) {
        var u = usuarios.findByUsernameIgnoreCase(a.getName())
                .orElseThrow(() -> new NoEncontradoException("Usuario", a.getName()));
        return new Yo(u.getUsername(), u.getNombreCompleto(), u.getRol().name());
    }
}
