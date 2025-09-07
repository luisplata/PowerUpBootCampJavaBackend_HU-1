package com.peryloth.jwtvalidation;

import com.peryloth.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.*;

import java.util.Date;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Component
@RequiredArgsConstructor
public class JwtValidacionAdapter implements IValidateJwt, IJwtTokenProvider, PasswordEncoder, ILogin {
    private static final Logger log = LoggerFactory.getLogger(JwtValidacionAdapter.class);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final UsuarioRepository usuarioRepository;

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> validate(String jwt) {
        return Mono.justOrEmpty(jwt)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Token vacío")))
                .flatMap(this::validateTokenReactive)
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        return Mono.empty(); // éxito
                    } else {
                        return Mono.error(new IllegalArgumentException("Token inválido"));
                    }
                });
    }

    public Mono<String> createToken(String email) {
        return Mono.fromSupplier(() ->
                Jwts.builder()
                        .setSubject(email)
                        .setIssuer("hu2-service")
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                        .signWith(jwtProperties.getKey(), SignatureAlgorithm.HS256)
                        .compact()
        );
    }

    @Override
    public Mono<String> getUsernameFromToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                String cleanToken = token.replace("Bearer ", "").trim();
                Claims claims = Jwts.parser() // ✅ en 0.13.0
                        .setSigningKey(jwtProperties.getKey())
                        .build()
                        .parseClaimsJws(cleanToken)
                        .getBody();
                return claims.getSubject();
            } catch (JwtException | IllegalArgumentException e) {
                log.error("❌ Error al extraer el username del token: {}", e.getMessage());
                return null;
            }
        });
    }


    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public Mono<String> login(String email, String password) {
        return usuarioRepository.getUsuarioByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                .flatMap(usuario -> {
                    System.out.println("Login - email: " + usuario.getEmail());
                    System.out.println("Login - password enviado: " + password);
                    System.out.println("Login - password hash en DB: " + usuario.getPasswordHash());
                    if (this.matches(password, usuario.getPasswordHash())) {
                        return this.createToken(usuario.getEmail());
                    }
                    return Mono.error(new IllegalArgumentException("Credenciales inválidas"));
                });
    }

    private Mono<Boolean> validateTokenReactive(String token) {
        return Mono.fromCallable(() -> {
            try {
                String cleanToken = token.replace("Bearer ", "").trim();

                var jws = Jwts.parser()
                        .setSigningKey(jwtProperties.getKey())
                        .build()
                        .parseClaimsJws(cleanToken);

                log.info("✅ Token válido. Subject={}, Issuer={}, Expiration={}",
                        jws.getBody().getSubject(),
                        jws.getBody().getIssuer(),
                        jws.getBody().getExpiration());

                return true;
            } catch (ExpiredJwtException e) {
                log.warn("⚠️ Token expirado: {}", e.getMessage());
                return false;
            } catch (JwtException | IllegalArgumentException e) {
                log.error("❌ Token inválido: {}", e.getMessage());
                return false;
            }
        });
    }


}
