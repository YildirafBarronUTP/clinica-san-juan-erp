package pe.clinicasanjuan.erp.pacientes.model;

import java.util.EnumSet;
import java.util.Set;

/** Máquina de estados del paciente. FALLECIDO es un estado final. */
public enum EstadoPaciente {
    ACTIVO, INACTIVO, FALLECIDO;

    public Set<EstadoPaciente> siguientesPermitidos() {
        return switch (this) {
            case ACTIVO -> EnumSet.of(INACTIVO, FALLECIDO);
            case INACTIVO -> EnumSet.of(ACTIVO, FALLECIDO);
            case FALLECIDO -> EnumSet.noneOf(EstadoPaciente.class);
        };
    }

    public boolean puedeCambiarA(EstadoPaciente destino) {
        return siguientesPermitidos().contains(destino);
    }
}
