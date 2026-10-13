package pe.clinicasanjuan.erp.pacientes.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alergia")
public class Alergia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(name = "principio_activo", nullable = false)
    private String principioActivo;

    private String reaccion;

    protected Alergia() {}

    Alergia(Paciente paciente, String principioActivo, String reaccion) {
        this.paciente = paciente;
        this.principioActivo = principioActivo;
        this.reaccion = reaccion;
    }

    public Long getId() { return id; }
    public String getPrincipioActivo() { return principioActivo; }
    public String getReaccion() { return reaccion; }
}
