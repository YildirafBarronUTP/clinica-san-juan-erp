package pe.clinicasanjuan.erp.pacientes.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "paciente")
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "historia_clinica", nullable = false, unique = true, updatable = false)
    private String historiaClinica;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 1)
    private String sexo;

    private String telefono;
    private String email;
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPaciente estado = EstadoPaciente.ACTIVO;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("principioActivo")
    private List<Alergia> alergias = new ArrayList<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    private long version;

    protected Paciente() {}

    public Paciente(String historiaClinica, String dni, String nombres, String apellidos, LocalDate fechaNacimiento,
                    String sexo, Instant ahora) {
        this.historiaClinica = historiaClinica;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public boolean tieneAlergiaA(String principioActivo) {
        String buscado = normalizar(principioActivo);
        return alergias.stream().anyMatch(a -> normalizar(a.getPrincipioActivo()).equals(buscado));
    }

    public Alergia agregarAlergia(String principioActivo, String reaccion) {
        Alergia a = new Alergia(this, principioActivo.trim(), reaccion);
        alergias.add(a);
        return a;
    }

    public static String normalizar(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getHistoriaClinica() { return historiaClinica; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public EstadoPaciente getEstado() { return estado; }
    public void setEstado(EstadoPaciente estado) { this.estado = estado; }
    public List<Alergia> getAlergias() { return alergias; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
    public long getVersion() { return version; }
}
