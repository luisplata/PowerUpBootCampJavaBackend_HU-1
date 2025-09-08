package com.peryloth.api;

import com.peryloth.api.dto.registry.UsuarioRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private final AuthFilter authFilter;

    public RouterRest(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/usuarios",
                    beanClass = Handler.class,
                    beanMethod = "saveUser",
                    operation = @Operation(
                            operationId = "saveUser",
                            summary = "Registrar un nuevo usuario",
                            description = "Recibe un objeto UsuarioRequestDTO y guarda un usuario en el sistema",
                            requestBody = @RequestBody(
                                    required = true,
                                    description = "Datos del usuario a registrar",
                                    content = @Content(schema = @Schema(implementation = UsuarioRequestDTO.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Usuario guardado correctamente",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                                    @ApiResponse(responseCode = "500", description = "Error interno",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/users/validate",
                    beanClass = Handler.class,
                    beanMethod = "validateUser",
                    operation = @Operation(
                            operationId = "validateUser",
                            summary = "Validar un usuario",
                            description = "Valida las credenciales de un usuario y retorna el resultado de la validación.",
                            requestBody = @RequestBody(
                                    required = true,
                                    description = "Datos del usuario a validar",
                                    content = @Content(schema = @Schema(implementation = UsuarioRequestDTO.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Usuario válido",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                                    @ApiResponse(responseCode = "401", description = "Usuario o token inválido",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST("/api/v1/usuarios"), handler::saveUser)
                .andRoute(POST("/api/v1/users/validate"), handler::validateUser)
                .andRoute(POST("/api/v1/login"), handler::login)
                .andNest(path("/api/v1/usuarios/admin"),
                        route(POST(""), handler::saveAdmin)
                )
                .andNest(path("/api/v1/token/validate"),
                        route(GET(""), handler::validateToken)
                                .filter(authFilter)
                )
                .andNest(path("/api/v1/users/getUser"),
                        route(POST(""), handler::getUser)
                                .filter(authFilter)
                );

    }
}
