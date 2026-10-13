package pe.clinicasanjuan.erp.farmacia.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.clinicasanjuan.erp.comun.Pagina;
import pe.clinicasanjuan.erp.farmacia.model.Lote;
import pe.clinicasanjuan.erp.farmacia.model.MovimientoKardex;
import pe.clinicasanjuan.erp.farmacia.model.Producto;
import pe.clinicasanjuan.erp.farmacia.service.InventarioService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FarmaciaController {
    private final InventarioService servicio;

    public FarmaciaController(InventarioService servicio) {
        this.servicio = servicio;
    }

    public record ProductoRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{3,20}$", message = "3 a 20 caracteres: letras, números o guion") String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 80) String principioActivo,
            @NotBlank @Size(max = 60) String presentacion,
            @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0") @Digits(integer = 8, fraction = 2) BigDecimal precioVenta,
            @NotNull @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
            Boolean activo) {}

    public record LoteRequest(
            @NotBlank @Size(max = 30) String numeroLote,
            @NotNull LocalDate fechaVencimiento,
            @NotNull Integer cantidad,
            @NotNull @Digits(integer = 6, fraction = 4) BigDecimal costoUnitario) {}

    public record SalidaRequest(@NotNull Integer cantidad, @NotBlank @Size(max = 100) String referencia) {}

    public record AjusteRequest(@NotNull Long loteId, @NotNull Integer cantidad, @Size(max = 100) String motivo) {}

    public record ProductoDto(Long id, String codigo, String nombre, String principioActivo, String presentacion,
                              BigDecimal precioVenta, BigDecimal costoPromedio, int stockMinimo, boolean activo,
                              int stockFisico, int stockDisponible) {}

    public record LoteDto(Long id, String numeroLote, LocalDate fechaVencimiento, int cantidadInicial,
                          int cantidadActual, BigDecimal costoUnitario, Instant fechaIngreso) {
        static LoteDto de(Lote l) {
            return new LoteDto(l.getId(), l.getNumeroLote(), l.getFechaVencimiento(), l.getCantidadInicial(),
                    l.getCantidadActual(), l.getCostoUnitario(), l.getFechaIngreso());
        }
    }

    public record MovimientoDto(Long id, String tipo, String numeroLote, int cantidad, int saldo,
                                BigDecimal costoUnitario, String referencia, String usuario, Instant fecha) {
        static MovimientoDto de(MovimientoKardex m) {
            return new MovimientoDto(m.getId(), m.getTipo().name(), m.getLote() == null ? null : m.getLote().getNumeroLote(),
                    m.getCantidad(), m.getSaldo(), m.getCostoUnitario(), m.getReferencia(), m.getUsuario(), m.getFecha());
        }
    }

    private ProductoDto dto(Producto p) {
        var s = servicio.stock(p.getId());
        return new ProductoDto(p.getId(), p.getCodigo(), p.getNombre(), p.getPrincipioActivo(), p.getPresentacion(),
                p.getPrecioVenta(), p.getCostoPromedio(), p.getStockMinimo(), p.isActivo(), s.fisico(), s.disponible());
    }

    // ---- Productos ----

    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA','MEDICO','CAJA')")
    public Pagina<ProductoDto> buscar(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean soloActivos,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return Pagina.de(servicio.buscarProductos(q, soloActivos, page, size), this::dto);
    }

    @GetMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA','MEDICO','CAJA')")
    public ProductoDto obtener(@PathVariable Long id) {
        return dto(servicio.producto(id));
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public ProductoDto crear(@Valid @RequestBody ProductoRequest r) {
        return dto(servicio.crearProducto(r.codigo(), r.nombre(), r.principioActivo(), r.presentacion(),
                r.precioVenta(), r.stockMinimo()));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public ProductoDto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
        return dto(servicio.actualizarProducto(id, r.nombre(), r.principioActivo(), r.presentacion(),
                r.precioVenta(), r.stockMinimo(), r.activo() == null || r.activo()));
    }

    // ---- Lotes y movimientos ----

    @GetMapping("/productos/{id}/lotes")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public List<LoteDto> lotes(@PathVariable Long id) {
        return servicio.lotesDe(id).stream().map(LoteDto::de).toList();
    }

    @PostMapping("/productos/{id}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public LoteDto ingresarLote(@PathVariable Long id, @Valid @RequestBody LoteRequest r) {
        return LoteDto.de(servicio.ingresarLote(id, r.numeroLote(), r.fechaVencimiento(), r.cantidad(), r.costoUnitario()));
    }

    /** Salida directa por FEFO. La dispensación por receta (Sprint 2) reutiliza este mismo servicio. */
    @PostMapping("/productos/{id}/salidas")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public List<InventarioService.DetalleSalida> salida(@PathVariable Long id, @Valid @RequestBody SalidaRequest r) {
        return servicio.dispensar(id, r.cantidad(), r.referencia().trim());
    }

    @PostMapping("/productos/{id}/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public MovimientoDto ajustar(@PathVariable Long id, @Valid @RequestBody AjusteRequest r) {
        return MovimientoDto.de(servicio.ajustar(id, r.loteId(), r.cantidad(), r.motivo()));
    }

    @GetMapping("/productos/{id}/kardex")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public List<MovimientoDto> kardex(@PathVariable Long id) {
        return servicio.kardex(id).stream().map(MovimientoDto::de).toList();
    }

    // ---- Inventario ----

    @GetMapping("/inventario/valorizacion")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public InventarioService.Valorizacion valorizacion() {
        return servicio.valorizacion();
    }

    @GetMapping("/inventario/alertas")
    @PreAuthorize("hasAnyRole('ADMIN','FARMACIA')")
    public InventarioService.Alertas alertas() {
        return servicio.alertas();
    }
}
