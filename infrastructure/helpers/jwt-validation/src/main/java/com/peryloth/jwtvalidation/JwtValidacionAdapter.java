package com.peryloth.jwtvalidation;

import com.peryloth.jwtvalidation.login.ILogin;
import com.peryloth.jwtvalidation.login.PasswordEncoder;
import com.peryloth.jwtvalidation.login.IJwtTokenProvider;
import com.peryloth.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Component
@RequiredArgsConstructor
public class JwtValidacionAdapter implements IValidateJwt, IJwtTokenProvider, PasswordEncoder, ILogin {

    private static final long EXPIRATION_TIME = 3600_000;

    private static final Logger log = LoggerFactory.getLogger(JwtValidacionAdapter.class);

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Key KEY_BY_MICRO = Keys.hmacShaKeyFor(JwtProperties.SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    private final UsuarioRepository usuarioRepository;

    @Override
    public Mono<Void> validate(String jwt) {
        return Mono.justOrEmpty(jwt)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Token vacío")))
                .flatMap(token -> JwtTokenProvider.validateTokenReactive(token) // devuelve Mono<Boolean>
                        .flatMap(isValid -> {
                            if (Boolean.TRUE.equals(isValid)) {
                                return Mono.empty(); // éxito
                            } else {
                                return Mono.error(new IllegalArgumentException("Token inválido"));
                            }
                        })
                );
    }

    public Mono<String> createToken(String email) {
        return Mono.fromSupplier(() ->
                Jwts.builder()
                        .setSubject(email)
                        .setIssuer("hu2-service")
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                        .signWith(KEY_BY_MICRO, SignatureAlgorithm.HS256)
                        .compact()
        );
    }

    @Override
    public Mono<String> getUsernameFromToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parser() // ✅ en 0.13.0
                        .setSigningKey(KEY_BY_MICRO)
                        .build()
                        .parseClaimsJws(token)
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
}
