package pe.clinicasanjuan.erp.farmacia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.clinicasanjuan.erp.auditoria.AuditoriaService;
import pe.clinicasanjuan.erp.comun.ConflictoException;
import pe.clinicasanjuan.erp.comun.NoEncontradoException;
import pe.clinicasanjuan.erp.farmacia.model.Lote;
import pe.clinicasanjuan.erp.farmacia.model.MovimientoKardex;
import pe.clinicasanjuan.erp.farmacia.model.Producto;
import pe.clinicasanjuan.erp.farmacia.model.TipoMovimiento;
import pe.clinicasanjuan.erp.farmacia.repository.LoteRepository;
import pe.clinicasanjuan.erp.farmacia.repository.MovimientoKardexRepository;
import pe.clinicasanjuan.erp.farmacia.repository.ProductoRepository;
import pe.clinicasanjuan.erp.farmacia.service.InventarioService;
import pe.clinicasanjuan.erp.fixtures.Relojes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pe.clinicasanjuan.erp.fixtures.Relojes.HOY;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {
    @Mock ProductoRepository productos;
    @Mock LoteRepository lotes;
    @Mock MovimientoKardexRepository kardex;
    @Mock AuditoriaService auditoria;
    InventarioService servicio;
    Producto paracetamol;
    List<MovimientoKardex> movimientos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        servicio = new InventarioService(productos, lotes, kardex, auditoria,
                new InventarioService.FarmaciaProperties(30), Relojes.fijo());
        paracetamol = new Producto("MED-001", "Paracetamol 500 mg", "paracetamol", "Tableta", new BigDecimal("0.50"), 10);
        ReflectionTestUtils.setField(paracetamol, "id", 1L);
        paracetamol.setCostoPromedio(new BigDecimal("0.3000"));
        lenient().when(productos.findParaMovimiento(1L)).thenReturn(Optional.of(paracetamol));
        lenient().when(kardex.save(any())).thenAnswer(i -> {
            movimientos.add(i.getArgument(0));
            return i.getArgument(0);
        });
    }

    private Lote lote(long id, String numero, int diasParaVencer, int cantidad) {
        Lote l = new Lote(paracetamol, numero, HOY.plusDays(diasParaVencer), cantidad, new BigDecimal("0.30"), Instant.now());
        ReflectionTestUtils.setField(l, "id", id);
        return l;
    }

    @Test
    @DisplayName("CP-FAR-010 FEFO: consume primero el lote que vence antes y reparte entre lotes")
    void fefo() {
        Lote vencePronto = lote(1, "A", 0, 5);   // vence hoy: todavía se puede usar
        Lote venceLuego = lote(2, "B", 90, 20);
        when(lotes.disponiblesFefo(1L, HOY)).thenReturn(List.of(vencePronto, venceLuego));
        when(lotes.stockFisico(1L)).thenReturn(25);

        var detalle = servicio.dispensar(1L, 8, "Receta 15");

        assertThat(detalle).extracting(InventarioService.DetalleSalida::numeroLote, InventarioService.DetalleSalida::cantidad)
                .containsExactly(tuple("A", 5), tuple("B", 3));
        assertThat(vencePronto.getCantidadActual()).isZero();
        assertThat(venceLuego.getCantidadActual()).isEqualTo(17);
        assertThat(movimientos).extracting(MovimientoKardex::getTipo, MovimientoKardex::getSaldo)
                .containsExactly(tuple(TipoMovimiento.SALIDA, 20), tuple(TipoMovimiento.SALIDA, 17));
    }

    @Test
    @DisplayName("CP-FAR-011 Valor límite: dispensar exactamente todo el stock disponible")
    void dispensarTodo() {
        when(lotes.disponiblesFefo(1L, HOY)).thenReturn(List.of(lote(1, "A", 10, 4)));
        when(lotes.stockFisico(1L)).thenReturn(4);
        assertThat(servicio.dispensar(1L, 4, "R")).hasSize(1);
        assertThat(movimientos.get(0).getSaldo()).isZero();
    }

    @Test
    @DisplayName("CP-FAR-012 Stock insuficiente: una unidad más que lo disponible no descuenta nada")
    void stockInsuficiente() {
        Lote l = lote(1, "A", 10, 4);
        when(lotes.disponiblesFefo(1L, HOY)).thenReturn(List.of(l));
        assertThatThrownBy(() -> servicio.dispensar(1L, 5, "R"))
                .extracting("codigo").isEqualTo("STOCK_INSUFICIENTE");
        assertThat(l.getCantidadActual()).isEqualTo(4);
        verifyNoInteractions(kardex);
    }

    @Test
    @DisplayName("CP-FAR-013 Cantidad cero o negativa es inválida")
    void cantidadInvalida() {
        assertThatThrownBy(() -> servicio.dispensar(1L, 0, "R")).extracting("codigo").isEqualTo("CANTIDAD_INVALIDA");
        assertThatThrownBy(() -> servicio.dispensar(1L, -3, "R")).extracting("codigo").isEqualTo("CANTIDAD_INVALIDA");
    }

    @Test
    @DisplayName("CP-FAR-014 Ingreso de lote recalcula costo promedio y registra ENTRADA")
    void ingresoLote() {
        when(lotes.stockFisico(1L)).thenReturn(100);
        when(lotes.save(any())).thenAnswer(i -> i.getArgument(0));

        Lote l = servicio.ingresarLote(1L, " l-77 ", HOY.plusDays(1), 50, new BigDecimal("0.60"));

        assertThat(l.getNumeroLote()).isEqualTo("L-77");
        // (100 * 0.30 + 50 * 0.60) / 150 = 0.40
        assertThat(paracetamol.getCostoPromedio()).isEqualByComparingTo("0.4000");
        assertThat(movimientos).singleElement()
                .satisfies(m -> {
                    assertThat(m.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
                    assertThat(m.getSaldo()).isEqualTo(150);
                });
    }

    @Test
    @DisplayName("CP-FAR-015 Valor límite: no se ingresa un lote que vence hoy")
    void ingresoVenceHoy() {
        assertThatThrownBy(() -> servicio.ingresarLote(1L, "X", HOY, 10, BigDecimal.ONE))
                .extracting("codigo").isEqualTo("VENCIMIENTO_INVALIDO");
    }

    @Test
    @DisplayName("CP-FAR-016 Ingreso con cantidad o costo no positivos es rechazado")
    void ingresoInvalido() {
        assertThatThrownBy(() -> servicio.ingresarLote(1L, "X", HOY.plusDays(5), 0, BigDecimal.ONE))
                .extracting("codigo").isEqualTo("CANTIDAD_INVALIDA");
        assertThatThrownBy(() -> servicio.ingresarLote(1L, "X", HOY.plusDays(5), 1, BigDecimal.ZERO))
                .extracting("codigo").isEqualTo("COSTO_INVALIDO");
    }

    @Test
    @DisplayName("CP-FAR-017 Número de lote duplicado para el mismo producto")
    void loteDuplicado() {
        when(lotes.existsByProductoIdAndNumeroLoteIgnoreCase(1L, "A1")).thenReturn(true);
        assertThatThrownBy(() -> servicio.ingresarLote(1L, "a1", HOY.plusDays(5), 1, BigDecimal.ONE))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("CP-FAR-018 No se ingresa stock a un producto inactivo")
    void productoInactivo() {
        paracetamol.setActivo(false);
        assertThatThrownBy(() -> servicio.ingresarLote(1L, "A", HOY.plusDays(5), 1, BigDecimal.ONE))
                .extracting("codigo").isEqualTo("PRODUCTO_INACTIVO");
    }

    @Test
    @DisplayName("CP-FAR-019 Ajuste exige motivo y no puede superar lo que tiene el lote")
    void ajuste() {
        Lote l = lote(7, "A", 10, 3);
        when(lotes.findById(7L)).thenReturn(Optional.of(l));
        when(lotes.stockFisico(1L)).thenReturn(3);

        assertThatThrownBy(() -> servicio.ajustar(1L, 7L, 1, " ")).extracting("codigo").isEqualTo("MOTIVO_OBLIGATORIO");
        assertThatThrownBy(() -> servicio.ajustar(1L, 7L, 4, "Rotura")).extracting("codigo").isEqualTo("STOCK_INSUFICIENTE");

        MovimientoKardex m = servicio.ajustar(1L, 7L, 2, "Rotura");
        assertThat(m.getTipo()).isEqualTo(TipoMovimiento.AJUSTE);
        assertThat(m.getSaldo()).isEqualTo(1);
        assertThat(l.getCantidadActual()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-FAR-020 Ajuste sobre un lote de otro producto se trata como inexistente")
    void ajusteLoteAjeno() {
        Producto otro = new Producto("MED-002", "Ibuprofeno", "ibuprofeno", "Tableta", BigDecimal.ONE, 1);
        ReflectionTestUtils.setField(otro, "id", 2L);
        Lote ajeno = new Lote(otro, "Z", HOY.plusDays(10), 5, BigDecimal.ONE, Instant.now());
        when(lotes.findById(9L)).thenReturn(Optional.of(ajeno));
        assertThatThrownBy(() -> servicio.ajustar(1L, 9L, 1, "x")).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-FAR-021 Alertas: bajo mínimo, por vencer (30 días) y vencidos")
    void alertas() {
        when(lotes.stocksPorProducto(HOY)).thenReturn(List.<Object[]>of(new Object[]{1L, 30L, 10L}));
        when(productos.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(paracetamol));
        Lote vencido = lote(1, "V", -1, 20);
        Lote porVencer = lote(2, "P", 30, 10);
        when(lotes.conSaldoQueVencenHasta(HOY.plusDays(30))).thenReturn(List.of(vencido, porVencer));

        var a = servicio.alertas();

        assertThat(a.bajoMinimo()).extracting(InventarioService.AlertaStock::stockDisponible).containsExactly(10);
        assertThat(a.vencidos()).extracting(InventarioService.AlertaLote::numeroLote).containsExactly("V");
        assertThat(a.porVencer()).extracting(InventarioService.AlertaLote::diasRestantes).containsExactly(30L);
    }

    @Test
    @DisplayName("CP-FAR-022 Valorización suma stock físico por costo promedio y omite productos sin stock")
    void valorizacion() {
        Producto sinStock = new Producto("MED-026", "Insulina", "insulina", "Frasco", BigDecimal.TEN, 1);
        ReflectionTestUtils.setField(sinStock, "id", 2L);
        when(lotes.stocksPorProducto(HOY)).thenReturn(List.<Object[]>of(new Object[]{1L, 7L, 7L}));
        when(productos.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(paracetamol, sinStock));
        paracetamol.setCostoPromedio(new BigDecimal("1.2345"));

        var v = servicio.valorizacion();

        assertThat(v.lineas()).hasSize(1);
        assertThat(v.total()).isEqualTo(new BigDecimal("8.64"));
    }

    @Test
    @DisplayName("CP-FAR-023 Crear producto normaliza el código y rechaza duplicados")
    void crearProducto() {
        when(productos.existsByCodigoIgnoreCase("med-100")).thenReturn(false);
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(servicio.crearProducto("med-100", " Nuevo ", "x", "Tableta", BigDecimal.ONE, 5).getCodigo())
                .isEqualTo("MED-100");

        when(productos.existsByCodigoIgnoreCase("MED-001")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crearProducto("MED-001", "n", "x", "t", BigDecimal.ONE, 1))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("CP-FAR-024 Actualizar producto y consultar stock y kardex")
    void actualizarYConsultar() {
        when(productos.findById(1L)).thenReturn(Optional.of(paracetamol));
        servicio.actualizarProducto(1L, "Paracetamol 1 g", "paracetamol", "Tableta", BigDecimal.TEN, 20, false);
        assertThat(paracetamol.getNombre()).isEqualTo("Paracetamol 1 g");
        assertThat(paracetamol.isActivo()).isFalse();

        when(lotes.stockFisico(1L)).thenReturn(12);
        when(lotes.stockDisponible(1L, HOY)).thenReturn(9);
        assertThat(servicio.stock(1L)).isEqualTo(new InventarioService.Stock(12, 9));

        when(kardex.deProducto(1L)).thenReturn(List.of());
        assertThat(servicio.kardex(1L)).isEmpty();
        when(lotes.findByProductoIdOrderByFechaVencimientoAscIdAsc(1L)).thenReturn(List.of());
        assertThat(servicio.lotesDe(1L)).isEmpty();
        when(productos.buscar(eq("x"), eq(true), any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertThat(servicio.buscarProductos(" x ", true, 0, 10)).isEmpty();
    }
}
