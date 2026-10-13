package pe.clinicasanjuan.erp.pacientes.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.clinicasanjuan.erp.comun.Pagina;
import pe.clinicasanjuan.erp.pacientes.model.Alergia;
import pe.clinicasanjuan.erp.pacientes.model.EstadoPaciente;
import pe.clinicasanjuan.erp.pacientes.model.Paciente;
import pe.clinicasanjuan.erp.pacientes.service.DatosPaciente;
import pe.clinicasanjuan.erp.pacientes.service.PacienteService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pacientes")
public class PacienteController {
    private final PacienteService servicio;

    public PacienteController(PacienteService servicio) {
        this.servicio = servicio;
    }

    public record PacienteRequest(
            @NotBlank(message = "El DNI es obligatorio") String dni,
            @NotBlank @Size(max = 80) String nombres,
            @NotBlank @Size(max = 80) String apellidos,
            @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento,
            @NotBlank @Pattern(regexp = "[MF]", message = "Sexo debe ser M o F") String sexo,
            @Pattern(regexp = "^$|^9\\d{8}$", message = "El teléfono debe tener 9 dígitos y empezar con 9") String telefono,
            @Email @Size(max = 120) String email,
            @Size(max = 200) String direccion) {
        DatosPaciente aDatos() {
            return new DatosPaciente(dni.trim(), nombres, apellidos, fechaNacimiento, sexo, telefono, email, direccion);
        }
    }

    public record AlergiaRequest(@NotBlank @Size(max = 80) String principioActivo, @Size(max = 200) String reaccion) {}
    public record EstadoRequest(@NotNull EstadoPaciente estado) {}

    public record AlergiaDto(Long id, String principioActivo, String reaccion) {
        static AlergiaDto de(Alergia a) { return new AlergiaDto(a.getId(), a.getPrincipioActivo(), a.getReaccion()); }
    }

    public record PacienteResumen(Long id, String historiaClinica, String dni, String nombres, String apellidos,
                                  LocalDate fechaNacimiento, String estado) {
        static PacienteResumen de(Paciente p) {
            return new PacienteResumen(p.getId(), p.getHistoriaClinica(), p.getDni(), p.getNombres(),
                    p.getApellidos(), p.getFechaNacimiento(), p.getEstado().name());
        }
    }

    public record PacienteDetalle(Long id, String historiaClinica, String dni, String nombres, String apellidos,
                                  LocalDate fechaNacimiento, String sexo, String telefono, String email,
                                  String direccion, String estado, List<String> estadosPermitidos,
                                  List<AlergiaDto> alergias) {
        static PacienteDetalle de(Paciente p) {
            return new PacienteDetalle(p.getId(), p.getHistoriaClinica(), p.getDni(), p.getNombres(), p.getApellidos(),
                    p.getFechaNacimiento(), p.getSexo(), p.getTelefono(), p.getEmail(), p.getDireccion(),
                    p.getEstado().name(),
                    p.getEstado().siguientesPermitidos().stream().map(Enum::name).sorted().toList(),
                    p.getAlergias().stream().map(AlergiaDto::de).toList());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION','MEDICO','CAJA')")
    public Pagina<PacienteResumen> buscar(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) EstadoPaciente estado,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return Pagina.de(servicio.buscar(q, estado, page, size), PacienteResumen::de);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION','MEDICO')")
    public PacienteDetalle obtener(@PathVariable Long id) {
        return PacienteDetalle.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION')")
    public PacienteDetalle registrar(@Valid @RequestBody PacienteRequest req) {
        return PacienteDetalle.de(servicio.registrar(req.aDatos()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION')")
    public PacienteDetalle actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest req) {
        return PacienteDetalle.de(servicio.actualizar(id, req.aDatos()));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION')")
    public PacienteDetalle cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return PacienteDetalle.de(servicio.cambiarEstado(id, req.estado()));
    }

    @PostMapping("/{id}/alergias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ADMISION','MEDICO')")
    public AlergiaDto agregarAlergia(@PathVariable Long id, @Valid @RequestBody AlergiaRequest req) {
        return AlergiaDto.de(servicio.agregarAlergia(id, req.principioActivo(), req.reaccion()));
    }
}
