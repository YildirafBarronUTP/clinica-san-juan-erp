package pe.clinicasanjuan.erp.farmacia.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.comun.ReglaNegocioException;
import pe.clinicasanjuan.erp.farmacia.model.*;
import pe.clinicasanjuan.erp.farmacia.repository.LoteRepository;
import pe.clinicasanjuan.erp.farmacia.repository.MovimientoKardexRepository;
import pe.clinicasanjuan.erp.farmacia.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
public class InventarioService {

    @ConfigurationProperties(prefix = "app.farmacia")
    public record FarmaciaProperties(int diasAlertaVencimiento) {}

    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final MovimientoKardexRepository kardex;
    private final AuditoriaService auditoria;
    private final FarmaciaProperties props;
    private final Clock reloj;

    public InventarioService(ProductoRepository productos, LoteRepository lotes, MovimientoKardexRepository kardex,
                             AuditoriaService auditoria, FarmaciaProperties props, Clock reloj) {
        this.productos = productos;
        this.lotes = lotes;
        this.kardex = kardex;
        this.auditoria = auditoria;
        this.props = props;
        this.reloj = reloj;
    }

    // ---------------- Productos ----------------

    @Transactional
    public Producto crearProducto(String codigo, String nombre, String principioActivo, String presentacion,
                                  BigDecimal precioVenta, int stockMinimo) {
        if (productos.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoException("CODIGO_DUPLICADO", "Ya existe un producto con ese código", "codigo");
        }
        Producto p = productos.save(new Producto(codigo.trim().toUpperCase(Locale.ROOT), nombre.trim(),
                principioActivo.trim(), presentacion.trim(), precioVenta, stockMinimo));
        auditoria.registrar("CREAR", "Producto", p.getId(), p.getCodigo());
        return p;
    }

    @Transactional
    public Producto actualizarProducto(Long id, String nombre, String principioActivo, String presentacion,
                                       BigDecimal precioVenta, int stockMinimo, boolean activo) {
        Producto p = producto(id);
        p.setNombre(nombre.trim());
        p.setPrincipioActivo(principioActivo.trim());
        p.setPresentacion(presentacion.trim());
        p.setPrecioVenta(precioVenta);
        p.setStockMinimo(stockMinimo);
        p.setActivo(activo);
        auditoria.registrar("ACTUALIZAR", "Producto", p.getId(), null);
        return p;
    }

    @Transactional(readOnly = true)
    public Producto producto(Long id) {
        return productos.findById(id).orElseThrow(() -> new NoEncontradoException("Producto", id));
    }

    @Transactional(readOnly = true)
    public Page<Producto> buscarProductos(String q, boolean soloActivos, int pagina, int tamanio) {
        return productos.buscar(q == null ? "" : q.trim(), soloActivos,
                PageRequest.of(Math.max(pagina, 0), Math.max(1, Math.min(tamanio, 100)), Sort.by("nombre", "id")));
    }

    @Transactional(readOnly = true)
    public Stock stock(Long productoId) {
        LocalDate hoy = LocalDate.now(reloj);
        return new Stock(lotes.stockFisico(productoId), lotes.stockDisponible(productoId, hoy));
    }

    @Transactional(readOnly = true)
    public List<Lote> lotesDe(Long productoId) {
        producto(productoId);
        return lotes.findByProductoIdOrderByFechaVencimientoAscIdAsc(productoId);
    }

    // ---------------- Movimientos ----------------

    /** Ingreso de un lote: entrada en kardex y recálculo del costo promedio ponderado. */
    @Transactional
    public Lote ingresarLote(Long productoId, String numeroLote, LocalDate vencimiento, int cantidad,
                             BigDecimal costoUnitario) {
        LocalDate hoy = LocalDate.now(reloj);
        if (cantidad <= 0) {
            throw new ReglaNegocioException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor a 0", "cantidad");
        }
        if (costoUnitario == null || costoUnitario.signum() <= 0) {
            throw new ReglaNegocioException("COSTO_INVALIDO", "El costo unitario debe ser mayor a 0", "costoUnitario");
        }
        if (!vencimiento.isAfter(hoy)) {
            throw new ReglaNegocioException("VENCIMIENTO_INVALIDO", "No se puede ingresar un lote vencido o que vence hoy",
                    "fechaVencimiento");
        }
        Producto p = productoParaMovimiento(productoId);
        if (!p.isActivo()) {
            throw new ReglaNegocioException("PRODUCTO_INACTIVO", "El producto está inactivo");
        }
        String numero = numeroLote.trim().toUpperCase(Locale.ROOT);
        if (lotes.existsByProductoIdAndNumeroLoteIgnoreCase(productoId, numero)) {
            throw new ConflictoException("LOTE_DUPLICADO", "Ese número de lote ya existe para el producto", "numeroLote");
        }
        int stockAntes = lotes.stockFisico(productoId);
        p.setCostoPromedio(CalculosInventario.costoPromedio(stockAntes, p.getCostoPromedio(), cantidad, costoUnitario));
        Lote lote = lotes.save(new Lote(p, numero, vencimiento, cantidad, costoUnitario, reloj.instant()));
        registrarMovimiento(p, lote, TipoMovimiento.ENTRADA, cantidad, stockAntes + cantidad, costoUnitario,
                "Ingreso lote " + numero);
        auditoria.registrar("INGRESO_LOTE", "Producto", p.getId(), numero + " x" + cantidad);
        return lote;
    }

    /**
     * Salida por FEFO: consume primero los lotes que vencen antes; nunca usa lotes
     * vencidos ni deja stock negativo. Si otra transacción mueve el mismo producto a la
     * vez, una de las dos falla con 409 (bloqueo optimista sobre el producto).
     */
    @Transactional
    public List<DetalleSalida> dispensar(Long productoId, int cantidad, String referencia) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor a 0", "cantidad");
        }
        LocalDate hoy = LocalDate.now(reloj);
        Producto p = productoParaMovimiento(productoId);
        List<Lote> disponibles = lotes.disponiblesFefo(productoId, hoy);
        int totalDisponible = disponibles.stream().mapToInt(Lote::getCantidadActual).sum();
        if (totalDisponible < cantidad) {
            throw new ReglaNegocioException("STOCK_INSUFICIENTE",
                    "Stock disponible insuficiente: hay " + totalDisponible + ", se pidieron " + cantidad, "cantidad");
        }
        int saldo = lotes.stockFisico(productoId);
        int pendiente = cantidad;
        List<DetalleSalida> detalle = new ArrayList<>();
        for (Lote l : disponibles) {
            if (pendiente == 0) break;
            int tomar = Math.min(pendiente, l.getCantidadActual());
            l.descontar(tomar);
            saldo -= tomar;
            pendiente -= tomar;
            registrarMovimiento(p, l, TipoMovimiento.SALIDA, tomar, saldo, p.getCostoPromedio(), referencia);
            detalle.add(new DetalleSalida(l.getId(), l.getNumeroLote(), l.getFechaVencimiento(), tomar));
        }
        auditoria.registrar("DISPENSAR", "Producto", p.getId(), referencia + " x" + cantidad);
        return detalle;
    }

    /** Ajuste negativo (merma, rotura, vencimiento) sobre un lote concreto; motivo obligatorio. */
    @Transactional
    public MovimientoKardex ajustar(Long productoId, Long loteId, int cantidad, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("MOTIVO_OBLIGATORIO", "El ajuste requiere un motivo", "motivo");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("CANTIDAD_INVALIDA", "La cantidad debe ser mayor a 0", "cantidad");
        }
        Producto p = productoParaMovimiento(productoId);
        Lote l = lotes.findById(loteId)
                .filter(x -> x.getProducto().getId().equals(productoId))
                .orElseThrow(() -> new NoEncontradoException("Lote", loteId));
        if (cantidad > l.getCantidadActual()) {
            throw new ReglaNegocioException("STOCK_INSUFICIENTE",
                    "El lote solo tiene " + l.getCantidadActual() + " unidades", "cantidad");
        }
        int saldo = lotes.stockFisico(productoId) - cantidad;
        l.descontar(cantidad);
        MovimientoKardex m = registrarMovimiento(p, l, TipoMovimiento.AJUSTE, cantidad, saldo, p.getCostoPromedio(),
                "Ajuste: " + motivo.trim());
        auditoria.registrar("AJUSTE", "Producto", p.getId(), l.getNumeroLote() + " -" + cantidad + " " + motivo.trim());
        return m;
    }

    // ---------------- Consultas ----------------

    @Transactional(readOnly = true)
    public List<MovimientoKardex> kardex(Long productoId) {
        producto(productoId);
        return kardex.deProducto(productoId);
    }

    @Transactional(readOnly = true)
    public Valorizacion valorizacion() {
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, int[]> stocks = stocksPorProducto(hoy);
        List<LineaValorizacion> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(CalculosInventario.ESCALA_MONEDA);
        for (Producto p : productos.findAll(Sort.by("nombre", "id"))) {
            int[] s = stocks.get(p.getId());
            if (s == null || s[0] == 0) continue;
            BigDecimal valor = CalculosInventario.valorizar(s[0], p.getCostoPromedio());
            lineas.add(new LineaValorizacion(p.getId(), p.getCodigo(), p.getNombre(), s[0], p.getCostoPromedio(), valor));
            total = total.add(valor);
        }
        return new Valorizacion(hoy, lineas, total);
    }

    @Transactional(readOnly = true)
    public Alertas alertas() {
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, int[]> stocks = stocksPorProducto(hoy);
        List<AlertaStock> bajoMinimo = new ArrayList<>();
        for (Producto p : productos.findAll(Sort.by("nombre", "id"))) {
            if (!p.isActivo()) continue;
            int disponible = stocks.getOrDefault(p.getId(), new int[]{0, 0})[1];
            if (CalculosInventario.requiereReposicion(disponible, p.getStockMinimo())) {
                bajoMinimo.add(new AlertaStock(p.getId(), p.getCodigo(), p.getNombre(), disponible, p.getStockMinimo()));
            }
        }
        List<AlertaLote> porVencer = new ArrayList<>();
        List<AlertaLote> vencidos = new ArrayList<>();
        for (Lote l : lotes.conSaldoQueVencenHasta(hoy.plusDays(props.diasAlertaVencimiento()))) {
            AlertaLote a = new AlertaLote(l.getId(), l.getProducto().getId(), l.getProducto().getNombre(),
                    l.getNumeroLote(), l.getFechaVencimiento(), l.getCantidadActual(),
                    java.time.temporal.ChronoUnit.DAYS.between(hoy, l.getFechaVencimiento()));
            (l.estaVencido(hoy) ? vencidos : porVencer).add(a);
        }
        return new Alertas(hoy, bajoMinimo, porVencer, vencidos);
    }

    // ---------------- Internos ----------------

    private Producto productoParaMovimiento(Long id) {
        return productos.findParaMovimiento(id).orElseThrow(() -> new NoEncontradoException("Producto", id));
    }

    private MovimientoKardex registrarMovimiento(Producto p, Lote l, TipoMovimiento tipo, int cantidad, int saldo,
                                                 BigDecimal costo, String referencia) {
        return kardex.save(new MovimientoKardex(p, l, tipo, cantidad, saldo, costo, referencia,
                AuditoriaService.usuarioActual(), reloj.instant()));
    }

    private Map<Long, int[]> stocksPorProducto(LocalDate hoy) {
        Map<Long, int[]> m = new HashMap<>();
        for (Object[] r : lotes.stocksPorProducto(hoy)) {
            m.put((Long) r[0], new int[]{((Number) r[1]).intValue(), ((Number) r[2]).intValue()});
        }
        return m;
    }

    // ---------------- Tipos de resultado ----------------

    public record Stock(int fisico, int disponible) {}
    public record DetalleSalida(Long loteId, String numeroLote, LocalDate fechaVencimiento, int cantidad) {}
    public record LineaValorizacion(Long productoId, String codigo, String nombre, int stock, BigDecimal costoPromedio,
                                    BigDecimal valor) {}
    public record Valorizacion(LocalDate fecha, List<LineaValorizacion> lineas, BigDecimal total) {}
    public record AlertaStock(Long productoId, String codigo, String nombre, int stockDisponible, int stockMinimo) {}
    public record AlertaLote(Long loteId, Long productoId, String producto, String numeroLote, LocalDate fechaVencimiento,
                             int cantidad, long diasRestantes) {}
    public record Alertas(LocalDate fecha, List<AlertaStock> bajoMinimo, List<AlertaLote> porVencer,
                          List<AlertaLote> vencidos) {}
}
