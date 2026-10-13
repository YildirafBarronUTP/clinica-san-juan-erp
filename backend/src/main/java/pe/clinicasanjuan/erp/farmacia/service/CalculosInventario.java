package pe.clinicasanjuan.erp.farmacia.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos puros del inventario: sin base de datos, fáciles de probar con valores límite. */
public final class CalculosInventario {
    public static final int ESCALA_COSTO = 4;
    public static final int ESCALA_MONEDA = 2;

    private CalculosInventario() {}

    /**
     * Costo promedio ponderado tras un ingreso:
     * (stock * costoActual + cantidad * costoIngreso) / (stock + cantidad).
     */
    public static BigDecimal costoPromedio(int stockActual, BigDecimal costoActual, int cantidadIngreso,
                                           BigDecimal costoIngreso) {
        if (stockActual < 0 || cantidadIngreso <= 0) {
            throw new IllegalArgumentException("Cantidades inválidas");
        }
        if (stockActual == 0) {
            return costoIngreso.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
        }
        BigDecimal valorActual = costoActual.multiply(BigDecimal.valueOf(stockActual));
        BigDecimal valorIngreso = costoIngreso.multiply(BigDecimal.valueOf(cantidadIngreso));
        return valorActual.add(valorIngreso)
                .divide(BigDecimal.valueOf((long) stockActual + cantidadIngreso), ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    /** Valor de un stock a costo promedio, redondeado a céntimos. */
    public static BigDecimal valorizar(int stock, BigDecimal costoPromedio) {
        return costoPromedio.multiply(BigDecimal.valueOf(stock)).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
    }

    /** El producto entra en alerta cuando su stock disponible llega al mínimo o baja de él. */
    public static boolean requiereReposicion(int stockDisponible, int stockMinimo) {
        return stockDisponible <= stockMinimo;
    }
}
