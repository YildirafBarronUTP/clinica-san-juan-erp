package pe.clinicasanjuan.erp.pacientes.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;
import pe.clinicasanjuan.erp.pacientes.model.Alergia;
import pe.clinicasanjuan.erp.pacientes.model.EstadoPaciente;
import pe.clinicasanjuan.erp.pacientes.model.Paciente;
import pe.clinicasanjuan.erp.pacientes.repository.PacienteRepository;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class PacienteService {
    static final int EDAD_MAXIMA = 120;

    private final PacienteRepository repo;
    private final AuditoriaService auditoria;
    private final Clock reloj;

    public PacienteService(PacienteRepository repo, AuditoriaService auditoria, Clock reloj) {
        this.repo = repo;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Paciente registrar(DatosPaciente d) {
        validar(d);
        if (repo.existsByDni(d.dni())) {
            throw new ConflictoException("DNI_DUPLICADO", "Ya existe un paciente con ese DNI", "dni");
        }
        String hc = "HC-%06d".formatted(repo.siguienteHistoria());
        Paciente p = new Paciente(hc, d.dni(), limpiar(d.nombres()), limpiar(d.apellidos()),
                d.fechaNacimiento(), d.sexo(), reloj.instant());
        aplicarOpcionales(p, d);
        p = repo.save(p);
        auditoria.registrar("CREAR", "Paciente", p.getId(), hc);
        return p;
    }

    @Transactional
    public Paciente actualizar(Long id, DatosPaciente d) {
        Paciente p = obtener(id);
        if (p.getEstado() == EstadoPaciente.FALLECIDO) {
            throw new ReglaNegocioException("PACIENTE_FALLECIDO", "No se puede modificar el expediente de un paciente fallecido");
        }
        validar(d);
        if (!p.getDni().equals(d.dni()) && repo.existsByDniAndIdNot(d.dni(), id)) {
            throw new ConflictoException("DNI_DUPLICADO", "Ya existe un paciente con ese DNI", "dni");
        }
        String antes = p.getDni();
        p.setDni(d.dni());
        p.setNombres(limpiar(d.nombres()));
        p.setApellidos(limpiar(d.apellidos()));
        p.setFechaNacimiento(d.fechaNacimiento());
        p.setSexo(d.sexo());
        aplicarOpcionales(p, d);
        p.setActualizadoEn(reloj.instant());
        auditoria.registrar("ACTUALIZAR", "Paciente", p.getId(),
                antes.equals(d.dni()) ? null : "dni " + antes + " -> " + d.dni());
        return p;
    }

    @Transactional
    public Paciente cambiarEstado(Long id, EstadoPaciente destino) {
        Paciente p = obtener(id);
        if (!p.getEstado().puedeCambiarA(destino)) {
            throw new ReglaNegocioException("TRANSICION_INVALIDA",
                    "No se puede pasar de " + p.getEstado() + " a " + destino, "estado");
        }
        EstadoPaciente anterior = p.getEstado();
        p.setEstado(destino);
        p.setActualizadoEn(reloj.instant());
        auditoria.registrar("CAMBIAR_ESTADO", "Paciente", p.getId(), anterior + " -> " + destino);
        return p;
    }

    @Transactional
    public Alergia agregarAlergia(Long id, String principioActivo, String reaccion) {
        Paciente p = obtener(id);
        if (p.tieneAlergiaA(principioActivo)) {
            throw new ConflictoException("ALERGIA_DUPLICADA", "La alergia ya está registrada", "principioActivo");
        }
        Alergia a = p.agregarAlergia(principioActivo, reaccion);
        repo.flush();
        auditoria.registrar("AGREGAR_ALERGIA", "Paciente", p.getId(), principioActivo);
        return a;
    }

    @Transactional(readOnly = true)
    public Paciente obtener(Long id) {
        return repo.findConAlergiasById(id).orElseThrow(() -> new NoEncontradoException("Paciente", id));
    }

    @Transactional(readOnly = true)
    public Page<Paciente> buscar(String q, EstadoPaciente estado, int pagina, int tamanio) {
        String filtro = q == null ? "" : q.trim();
        int size = Math.max(1, Math.min(tamanio, 100));
        return repo.buscar(filtro, estado, PageRequest.of(Math.max(pagina, 0), size,
                Sort.by("apellidos", "nombres", "id")));
    }

    /** Reglas que Bean Validation no puede expresar sola (dependen de "hoy"). */
    void validar(DatosPaciente d) {
        if (!ValidadorDni.esValido(d.dni())) {
            throw new ReglaNegocioException("DNI_INVALIDO", "El DNI debe tener exactamente 8 dígitos", "dni");
        }
        LocalDate hoy = LocalDate.now(reloj);
        if (d.fechaNacimiento().isAfter(hoy)) {
            throw new ReglaNegocioException("FECHA_NACIMIENTO_FUTURA", "La fecha de nacimiento no puede ser futura", "fechaNacimiento");
        }
        if (d.fechaNacimiento().isBefore(hoy.minusYears(EDAD_MAXIMA))) {
            throw new ReglaNegocioException("FECHA_NACIMIENTO_INVALIDA", "La edad no puede superar " + EDAD_MAXIMA + " años", "fechaNacimiento");
        }
    }

    private static void aplicarOpcionales(Paciente p, DatosPaciente d) {
        p.setTelefono(vacioANulo(d.telefono()));
        p.setEmail(vacioANulo(d.email()));
        p.setDireccion(vacioANulo(d.direccion()));
    }

    private static String limpiar(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
