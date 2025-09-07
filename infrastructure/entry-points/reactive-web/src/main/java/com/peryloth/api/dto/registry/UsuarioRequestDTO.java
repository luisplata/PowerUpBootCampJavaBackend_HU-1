package com.peryloth.api.dto.registry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Builder(toBuilder = true)
public record UsuarioRequestDTO(

        @Schema(
                description = "Nombre del usuario",
                example = "Carlos",
                minLength = 2,
                maxLength = 50
        )
        String nombre,

        @Schema(
                description = "Apellido del usuario",
                example = "Pérez",
                minLength = 2,
                maxLength = 50
        )
        String apellido,

        @Schema(
                description = "Correo electrónico del usuario",
                example = "carlos.perez@example.com",
                format = "email"
        )
        String email,

        @Schema(
                description = "Número de documento de identidad",
                example = "1234567890",
                minLength = 6,
                maxLength = 20
        )
        String documentoIdentidad,

        @Schema(
                description = "Teléfono de contacto del usuario",
                example = "+57 3001234567",
                pattern = "^\\+?[0-9 ]{7,15}$"
        )
        String telefono,

        @Schema(
                description = "Salario base del usuario",
                example = "2500000",
                minimum = "0"
        )
        Long salarioBase,
        @Schema(
                description = "Contraseña en texto plano",
                example = "miPassword123")
        String password
) {
}
