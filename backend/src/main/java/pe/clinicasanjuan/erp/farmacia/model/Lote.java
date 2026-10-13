package pe.clinicasanjuan.erp.farmacia.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lote")
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;
    @Column(name = "numero_lote", nullable = false)
    private String numeroLote;
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;
    @Column(name = "cantidad_inicial", nullable = false)
    private int cantidadInicial;
    @Column(name = "cantidad_actual", nullable = false)
    private int cantidadActual;
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;
    @Column(name = "fecha_ingreso", nullable = false)
    private Instant fechaIngreso;
    @Version
    private long version;

    protected Lote() {}

    public Lote(Producto producto, String numeroLote, LocalDate fechaVencimiento, int cantidad,
                BigDecimal costoUnitario, Instant fechaIngreso) {
        this.producto = producto;
        this.numeroLote = numeroLote;
        this.fechaVencimiento = fechaVencimiento;
        this.cantidadInicial = cantidad;
        this.cantidadActual = cantidad;
        this.costoUnitario = costoUnitario;
        this.fechaIngreso = fechaIngreso;
    }

    /** Un lote que vence hoy todavía se puede dispensar hoy. */
    public boolean estaVencido(LocalDate hoy) {
        return fechaVencimiento.isBefore(hoy);
    }

    public void descontar(int cantidad) {
        if (cantidad <= 0 || cantidad > cantidadActual) {
            throw new IllegalArgumentException("Cantidad a descontar inválida: " + cantidad);
        }
        cantidadActual -= cantidad;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public String getNumeroLote() { return numeroLote; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public int getCantidadInicial() { return cantidadInicial; }
    public int getCantidadActual() { return cantidadActual; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public Instant getFechaIngreso() { return fechaIngreso; }
}
