package pe.clinicasanjuan.erp.comun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** El reloj se inyecta para que las pruebas controlen "hoy" (vencimientos, bloqueos). */
@Configuration
public class RelojConfig {
    @Bean
    Clock reloj(@Value("${app.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
