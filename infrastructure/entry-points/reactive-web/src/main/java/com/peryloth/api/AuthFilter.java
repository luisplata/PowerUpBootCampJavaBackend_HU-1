package com.peryloth.api;

import com.peryloth.jwtvalidation.login.IJwtTokenProvider;
import com.peryloth.jwtvalidation.login.PasswordEncoder;
import com.peryloth.model.rol.gateways.RolRepository;
import com.peryloth.model.usuario.gateways.UsuarioRepository;
import com.peryloth.jwtvalidation.IValidateJwt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final IJwtTokenProvider jwtTokenProvider;
    private final IValidateJwt validateJwt;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public AuthFilter(IJwtTokenProvider jwtTokenProvider, IValidateJwt validateJwt, PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.rolRepository = rolRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.validateJwt = validateJwt;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String authHeader = request.headers().firstHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        try {
            return validateJwt.validate(authHeader).flatMap(isValid -> {
                if (Boolean.TRUE.equals(isValid)) {
                    return jwtTokenProvider.getUsernameFromToken(token).flatMap(email ->
                            usuarioRepository.getUsuarioByEmail(email)
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                                    .flatMap(usuario -> rolRepository.getRolById(usuario.getRol().getUniqueId())
                                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Rol no encontrado")))
                                            .flatMap(rol -> {
                                                System.out.println("Rol del usuario: " + rol.getNombre());
                                                if (rol.getUniqueId().intValue() != 1) {
                                                    return ServerResponse.status(HttpStatus.FORBIDDEN).build();
                                                }
                                                // Aquí puedes agregar lógica adicional para verificar permisos según el rol
                                                return next.handle(request); // 👈 SOLO pasa si usuario+rol existen
                                            })
                                    )
                    );
                } else {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                }
            });
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
