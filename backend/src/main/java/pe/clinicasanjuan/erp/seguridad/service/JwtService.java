package pe.clinicasanjuan.erp.seguridad.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.clinicasanjuan.erp.seguridad.model.Rol;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey clave;
    private final Duration vigencia;
    private final Clock reloj;

    public JwtService(JwtProperties props, Clock reloj) {
        if (props.secret() == null || props.secret().length() < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofMinutes(props.expiracionMinutos());
        this.reloj = reloj;
    }

    public TokenEmitido emitir(String username, Rol rol) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(vigencia);
        String token = Jwts.builder()
                .subject(username)
                .claim("rol", rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
        return new TokenEmitido(token, expira);
    }

    /** Devuelve vacío si el token está vencido, mal firmado o mal formado. */
    public Optional<Sesion> validar(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new Sesion(c.getSubject(), Rol.valueOf(c.get("rol", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenEmitido(String token, Instant expiraEn) {}
    public record Sesion(String username, Rol rol) {}
}
