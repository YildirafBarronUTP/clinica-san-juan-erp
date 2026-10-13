package pe.clinicasanjuan.erp.pacientes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.clinicasanjuan.erp.pacientes.model.EstadoPaciente;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba de transición de estados: la tabla completa 3x3. */
class EstadoPacienteTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @DisplayName("CP-PAC-010 Tabla de transiciones del paciente")
    @CsvSource({
            "ACTIVO,    ACTIVO,    false",
            "ACTIVO,    INACTIVO,  true",
            "ACTIVO,    FALLECIDO, true",
            "INACTIVO,  ACTIVO,    true",
            "INACTIVO,  INACTIVO,  false",
            "INACTIVO,  FALLECIDO, true",
            "FALLECIDO, ACTIVO,    false",
            "FALLECIDO, INACTIVO,  false",
            "FALLECIDO, FALLECIDO, false",
    })
    void transiciones(EstadoPaciente origen, EstadoPaciente destino, boolean permitido) {
        assertThat(origen.puedeCambiarA(destino)).isEqualTo(permitido);
    }
}
