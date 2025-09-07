package com.peryloth.api;

import com.peryloth.api.dto.getUsuer.GetUserByDataRequestDTO;
import com.peryloth.api.dto.getUsuer.GetUserByDataResponseDTO;
import com.peryloth.api.dto.login.LoginRequestDTO;
import com.peryloth.api.dto.login.LoginResponseDTO;
import com.peryloth.api.dto.registry.UserValidationRequest;
import com.peryloth.api.dto.registry.UsuarioRequestDTO;
import com.peryloth.api.dto.validateToken.ValidateTokenResponseDTO;
import com.peryloth.api.mapper.registry.UserDTOMapper;
import com.peryloth.jwtvalidation.IValidateJwt;
import com.peryloth.jwtvalidation.ILogin;
import com.peryloth.jwtvalidation.PasswordEncoder;
import com.peryloth.usecase.getusuerbyemail.IGetUsuerByEmailUseCase;
import com.peryloth.usecase.registry_user.IRegistryUserUseCase;
import com.peryloth.usecase.validationclient.IValidationClientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final IRegistryUserUseCase registryUserUseCase;
    private final UserDTOMapper userDTOMapper;
    private final IValidationClientUseCase validationClientUseCase;
    private final IGetUsuerByEmailUseCase iGetUsuerByEmailUseCase;
    private final PasswordEncoder passwordEncoder;
    private final ILogin loginUseCase;
    private final IValidateJwt jwtTokenProvider;

    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Recibe un objeto UsuarioRequestDTO y guarda un usuario en el sistema",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Usuario guardado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error de validación",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "500", description = "Error interno",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = String.class)))
            }
    )

    public Mono<ServerResponse> saveAdmin(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UsuarioRequestDTO.class)
                .doOnNext(dto -> log.debug("Payload recibido: {}", dto))
                .map(dto -> dto.toBuilder().password(passwordEncoder.encode(dto.password())).build())
                .flatMap(dto -> registryUserUseCase.registryUserAdmin(userDTOMapper.mapToEntity(dto))
                        .doOnSuccess(v -> log.info("Usuario registrado correctamente: {}", dto.email()))
                        .then(ServerResponse.ok().bodyValue("Usuario guardado correctamente"))
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Error de validación al registrar usuario: {}", e.getMessage());
                    return ServerResponse.badRequest().bodyValue("Error de validación: " + e.getMessage());
                })
                .onErrorResume(e -> {
                    log.error("Error interno al registrar usuario", e);
                    return ServerResponse.status(500).bodyValue("Error interno: " + e.getMessage());
                });
    }

    public Mono<ServerResponse> saveUser(ServerRequest serverRequest) {
        log.info("Iniciando proceso de registro de usuario");

        return serverRequest.bodyToMono(UsuarioRequestDTO.class)
                .doOnNext(dto -> log.debug("Payload recibido: {}", dto))
                .map(dto -> dto.toBuilder().password(passwordEncoder.encode(dto.password())).build())
                .flatMap(dto -> registryUserUseCase.registryNormalUser(userDTOMapper.mapToEntity(dto))
                        .doOnSuccess(v -> log.info("Usuario registrado correctamente: {}", dto.email()))
                        .then(ServerResponse.ok().bodyValue("Usuario guardado correctamente"))
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Error de validación al registrar usuario: {}", e.getMessage());
                    return ServerResponse.badRequest().bodyValue("Error de validación: " + e.getMessage());
                })
                .onErrorResume(e -> {
                    log.error("Error interno al registrar usuario", e);
                    return ServerResponse.status(500).bodyValue("Error interno: " + e.getMessage());
                });
    }

    public Mono<ServerResponse> validateUser(ServerRequest request) {
        log.info("Iniciando validación de usuario");

        return Mono.justOrEmpty(request.headers().firstHeader("Authorization"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Token no proporcionado")))
                .flatMap(jwtTokenProvider::validate)
                .then(request.bodyToMono(UserValidationRequest.class))
                .doOnNext(req -> log.debug("Payload validación recibido: {}", req))
                .flatMap(usuarioRequest ->
                        validationClientUseCase.isUserValid(usuarioRequest.getId(), usuarioRequest.getEmail())
                                .flatMap(isUserValid ->
                                        Boolean.TRUE.equals(isUserValid)
                                                ? ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(true)
                                                : ServerResponse.status(401).contentType(MediaType.APPLICATION_JSON).bodyValue(false)
                                )
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Error de validación: {}", e.getMessage());
                    return ServerResponse.status(401).bodyValue("Error de validación: " + e.getMessage());
                })
                .onErrorResume(e -> {
                    log.error("Error interno al validar usuario", e);
                    return ServerResponse.status(500).bodyValue("Error interno: " + e.getMessage());
                });
    }


    public Mono<ServerResponse> login(ServerRequest request) {
        log.info("Iniciando login de usuario");

        return request.bodyToMono(LoginRequestDTO.class)
                .doOnNext(dto -> log.debug("Payload login recibido: {}", dto))
                .flatMap(dto -> loginUseCase.login(dto.email(), dto.password())
                        .map(LoginResponseDTO::new)
                        .flatMap(resp -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(resp))
                )
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Error de credenciales: {}", e.getMessage());
                    return ServerResponse.status(401)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Error interno en login", e);
                    return ServerResponse.status(500)
                            .bodyValue(Map.of("error", "Error interno: " + e.getMessage()));
                });
    }

    public Mono<ServerResponse> validateToken(ServerRequest request) {
        ValidateTokenResponseDTO response = new ValidateTokenResponseDTO("OK");

        return ServerResponse.ok()
                .bodyValue(response);
    }

    public Mono<ServerResponse> getUser(ServerRequest request) {
        return request.bodyToMono(GetUserByDataRequestDTO.class).flatMap(dto ->
                iGetUsuerByEmailUseCase.getUserByEmailAndDocument(dto.email())
                        .flatMap(user -> {
                            GetUserByDataResponseDTO responseDto = new GetUserByDataResponseDTO(
                                    user.getEmail(),
                                    user.getNombre() + " " + user.getApellido(),
                                    user.getSalarioBase()
                            );
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(responseDto);
                        })
                        .switchIfEmpty(ServerResponse.status(404).bodyValue("Usuario no encontrado"))
        ).onErrorResume(e -> {
            log.error("Error al obtener usuario", e);
            return ServerResponse.status(500).bodyValue("Error interno: " + e.getMessage());
        });
    }
}
