package com.peryloth.api;

import com.peryloth.jwtvalidation.IJwtTokenProvider;
import com.peryloth.jwtvalidation.PasswordEncoder;
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

import java.util.Objects;

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
        try {
            return Mono.justOrEmpty(request.headers().firstHeader("Authorization"))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Token no proporcionado")))
                    .flatMap(validateJwt::validate)
                    .then(Mono.just(request.headers().firstHeader("Authorization")))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Token no proporcionado")))
                    .flatMap(auth -> jwtTokenProvider.getUsernameFromToken(auth).flatMap(email ->
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
                                    )
                                    //TODO: Crear otro filtro, solo para validar que sea token valido y no sacar info del usuario
                                    .switchIfEmpty(next.handle(request))
                    );
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
