package pe.clinicasanjuan.erp.seguridad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seguridad")
public record SeguridadProperties(int maxIntentosLogin, int minutosBloqueo) {}
