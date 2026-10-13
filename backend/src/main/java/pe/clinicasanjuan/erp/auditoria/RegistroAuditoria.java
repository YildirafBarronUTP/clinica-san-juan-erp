package pe.clinicasanjuan.erp.auditoria;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String usuario;
    @Column(nullable = false)
    private String accion;
    @Column(nullable = false)
    private String entidad;
    @Column(name = "entidad_id")
    private String entidadId;
    private String detalle;
    @Column(nullable = false)
    private Instant fecha;

    protected RegistroAuditoria() {}

    public RegistroAuditoria(String usuario, String accion, String entidad, String entidadId, String detalle, Instant fecha) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500);
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getDetalle() { return detalle; }
    public Instant getFecha() { return fecha; }
}
