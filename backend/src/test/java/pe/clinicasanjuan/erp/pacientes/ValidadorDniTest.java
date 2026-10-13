package pe.clinicasanjuan.erp.pacientes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.clinicasanjuan.erp.pacientes.service.ValidadorDni;

import static org.assertj.core.api.Assertions.assertThat;

/** Partición de equivalencia y valores límite sobre la longitud del DNI (7, 8, 9). */
class ValidadorDniTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" es válido")
    @DisplayName("CP-PAC-001 DNI de exactamente 8 dígitos es válido")
    @ValueSource(strings = {"12345678", "00000001", "99999999"})
    void validos(String dni) {
        assertThat(ValidadorDni.esValido(dni)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" es inválido")
    @DisplayName("CP-PAC-002 DNI fuera de formato es inválido")
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "123456789", "1234567A", " 12345678", "12 45678", "１２３４５６７８", "-1234567"})
    void invalidos(String dni) {
        assertThat(ValidadorDni.esValido(dni)).isFalse();
    }
}
