package pe.clinicasanjuan.erp.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SemillaProperties(String perfil, long semilla, String password) {
    public SemillaProperties {
        if (perfil == null) perfil = "none";
        if (password == null || password.isBlank()) password = "Clinica2026";
    }
}
