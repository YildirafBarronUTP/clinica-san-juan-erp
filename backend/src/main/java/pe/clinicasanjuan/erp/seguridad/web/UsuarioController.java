package pe.clinicasanjuan.erp.seguridad.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.clinicasanjuan.erp.seguridad.model.Rol;
import pe.clinicasanjuan.erp.seguridad.model.Usuario;
import pe.clinicasanjuan.erp.seguridad.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    public record UsuarioDto(Long id, String username, String nombreCompleto, String rol, boolean activo) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getRol().name(), u.isActivo());
        }
    }

    public record CrearUsuario(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._]{4,50}$", message = "4 a 50 caracteres: letras, números, punto o guion bajo") String username,
            @NotBlank String password,
            @NotBlank @Size(max = 120) String nombreCompleto,
            @NotNull Rol rol) {}

    public record CambioEstado(@NotNull Boolean activo) {}

    @GetMapping
    public List<UsuarioDto> listar() {
        return servicio.listar().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody CrearUsuario req) {
        return UsuarioDto.de(servicio.crear(req.username(), req.password(), req.nombreCompleto(), req.rol()));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstado req, Authentication a) {
        return UsuarioDto.de(servicio.cambiarEstado(id, req.activo(), a.getName()));
    }
}
