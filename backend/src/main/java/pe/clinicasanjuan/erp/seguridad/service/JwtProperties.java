package pe.clinicasanjuan.erp.seguridad.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiracionMinutos) {}
