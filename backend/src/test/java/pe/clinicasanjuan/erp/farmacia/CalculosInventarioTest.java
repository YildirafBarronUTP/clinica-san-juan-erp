package pe.clinicasanjuan.erp.farmacia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.clinicasanjuan.erp.farmacia.service.CalculosInventario;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculosInventarioTest {

    @ParameterizedTest(name = "stock {0} a {1} + {2} a {3} = {4}")
    @DisplayName("CP-FAR-001 Costo promedio ponderado")
    @CsvSource({
            // sin stock previo: toma el costo del ingreso
            "0,   0,      100, 2.50,   2.5000",
            // mismo costo: no cambia
            "100, 2.5000, 100, 2.50,   2.5000",
            // (100*2 + 50*3.5) / 150 = 2.5
            "100, 2.0000, 50,  3.50,   2.5000",
            // redondeo HALF_UP a 4 decimales: (1*1 + 2*2) / 3 = 1.66666...
            "1,   1.0000, 2,   2.00,   1.6667",
    })
    void costoPromedio(int stock, BigDecimal costo, int cantidad, BigDecimal costoIngreso, BigDecimal esperado) {
        assertThat(CalculosInventario.costoPromedio(stock, costo, cantidad, costoIngreso)).isEqualByComparingTo(esperado);
        assertThat(CalculosInventario.costoPromedio(stock, costo, cantidad, costoIngreso).scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("CP-FAR-002 Costo promedio rechaza cantidades inválidas")
    void costoPromedioInvalido() {
        assertThatThrownBy(() -> CalculosInventario.costoPromedio(-1, BigDecimal.ONE, 1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculosInventario.costoPromedio(1, BigDecimal.ONE, 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @DisplayName("CP-FAR-003 Valorización redondea a céntimos (HALF_UP)")
    @CsvSource({
            "0,   2.5000, 0.00",
            "3,   0.3333, 1.00",
            "7,   1.2345, 8.64",   // 8.6415 -> 8.64
            "5,   0.0010, 0.01",   // 0.0050 -> 0.01
            "10,  12.3456, 123.46",
    })
    void valorizar(int stock, BigDecimal costo, BigDecimal esperado) {
        assertThat(CalculosInventario.valorizar(stock, costo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "disponible {0}, mínimo {1} -> alerta {2}")
    @DisplayName("CP-FAR-004 Alerta de reposición en el límite del stock mínimo")
    @CsvSource({"9, 10, true", "10, 10, true", "11, 10, false", "0, 0, true", "1, 0, false"})
    void reposicion(int disponible, int minimo, boolean alerta) {
        assertThat(CalculosInventario.requiereReposicion(disponible, minimo)).isEqualTo(alerta);
    }
}
