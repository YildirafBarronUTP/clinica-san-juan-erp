package pe.clinicasanjuan.erp.fixtures;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Relojes fijos: "hoy" es controlado por la prueba. */
public final class Relojes {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    public static final LocalDate HOY = LocalDate.of(2026, 10, 13);

    private Relojes() {}

    public static Clock fijo() {
        return fijoEn(HOY);
    }

    public static Clock fijoEn(LocalDate dia) {
        return Clock.fixed(dia.atTime(10, 0).atZone(LIMA).toInstant(), LIMA);
    }
}
