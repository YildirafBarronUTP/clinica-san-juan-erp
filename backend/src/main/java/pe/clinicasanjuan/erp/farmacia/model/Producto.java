package pe.clinicasanjuan.erp.farmacia.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String codigo;
    @Column(nullable = false)
    private String nombre;
    @Column(name = "principio_activo", nullable = false)
    private String principioActivo;
    @Column(nullable = false)
    private String presentacion;
    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;
    /** Costo promedio ponderado, 4 decimales. */
    @Column(name = "costo_promedio", nullable = false)
    private BigDecimal costoPromedio = BigDecimal.ZERO;
    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;
    @Column(nullable = false)
    private boolean activo = true;
    @Version
    private long version;

    protected Producto() {}

    public Producto(String codigo, String nombre, String principioActivo, String presentacion,
                    BigDecimal precioVenta, int stockMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.principioActivo = principioActivo;
        this.presentacion = presentacion;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPrincipioActivo() { return principioActivo; }
    public void setPrincipioActivo(String principioActivo) { this.principioActivo = principioActivo; }
    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public BigDecimal getCostoPromedio() { return costoPromedio; }
    public void setCostoPromedio(BigDecimal costoPromedio) { this.costoPromedio = costoPromedio; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public long getVersion() { return version; }
}
