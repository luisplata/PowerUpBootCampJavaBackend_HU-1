package com.peryloth.api;

import com.peryloth.jwtvalidation.IJwtTokenProvider;
import com.peryloth.jwtvalidation.PasswordEncoder;
import com.peryloth.model.rol.Rol;
import com.peryloth.model.rol.gateways.RolRepository;
import com.peryloth.model.usuario.Usuario;
import com.peryloth.model.usuario.gateways.UsuarioRepository;
import com.peryloth.jwtvalidation.IValidateJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigInteger;

class AuthFilterTest {

    private IJwtTokenProvider jwtTokenProvider;
    private IValidateJwt validateJwt;
    private PasswordEncoder passwordEncoder;
    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private AuthFilter authFilter;

    @BeforeEach
    void setup() {
        jwtTokenProvider = Mockito.mock(IJwtTokenProvider.class);
        validateJwt = Mockito.mock(IValidateJwt.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        rolRepository = Mockito.mock(RolRepository.class);

        authFilter = new AuthFilter(jwtTokenProvider, validateJwt, passwordEncoder, usuarioRepository, rolRepository);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoToken() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test").build();

        // 👉 Crea directamente un ServerRequest con MockServerRequest
        ServerRequest request = MockServerRequest.builder()
                .exchange(MockServerWebExchange.from(httpRequest))
                .build();

        HandlerFunction<ServerResponse> next = r -> ServerResponse.ok().build();

        StepVerifier.create(authFilter.filter(request, next))
                .expectNextMatches(res -> res.statusCode().equals(HttpStatus.UNAUTHORIZED))
                .verifyComplete();
    }

    @Test
    void shouldPassWhenTokenValidAndUserAndRoleExist() {
        String token = "valid-token";
        String email = "user@test.com";

        // 👉 Construimos el request simulado con header Authorization
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        HandlerFunction<ServerResponse> next = r -> ServerResponse.ok().build();

        // mocks
        Mockito.when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(Mono.just(email));

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setRol(new Rol(BigInteger.ONE, "ADMIN", "ADMIN"));

        Mockito.when(usuarioRepository.getUsuarioByEmail(email)).thenReturn(Mono.just(usuario));
        Mockito.when(rolRepository.getRolById(BigInteger.ONE)).thenReturn(Mono.just(new Rol(BigInteger.ONE, "ADMIN", "ADMIN")));

        StepVerifier.create(authFilter.filter(request, next))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.OK))
                .verifyComplete();
    }
}
