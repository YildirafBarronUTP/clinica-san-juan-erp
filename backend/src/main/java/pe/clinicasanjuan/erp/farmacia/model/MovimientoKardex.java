package pe.clinicasanjuan.erp.farmacia.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movimiento_kardex")
public class MovimientoKardex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;
    @Column(nullable = false)
    private int cantidad;
    /** Stock físico total del producto después del movimiento. */
    @Column(nullable = false)
    private int saldo;
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;
    @Column(nullable = false)
    private String referencia;
    @Column(nullable = false)
    private String usuario;
    @Column(nullable = false)
    private Instant fecha;

    protected MovimientoKardex() {}

    public MovimientoKardex(Producto producto, Lote lote, TipoMovimiento tipo, int cantidad, int saldo,
                            BigDecimal costoUnitario, String referencia, String usuario, Instant fecha) {
        this.producto = producto;
        this.lote = lote;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.saldo = saldo;
        this.costoUnitario = costoUnitario;
        this.referencia = referencia;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public Lote getLote() { return lote; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getSaldo() { return saldo; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public String getReferencia() { return referencia; }
    public String getUsuario() { return usuario; }
    public Instant getFecha() { return fecha; }
}
