package com.peryloth.usecase.registry_user;

import com.peryloth.model.rol.gateways.RolRepository;
import com.peryloth.model.usuario.Usuario;
import com.peryloth.model.usuario.gateways.UsuarioRepository;
import com.peryloth.usecase.registry_user.command_queue.UsuarioValidationQueue;
import com.peryloth.usecase.registry_user.command_queue.validations.ApellidoValidation;
import com.peryloth.usecase.registry_user.command_queue.validations.EmailValidation;
import com.peryloth.usecase.registry_user.command_queue.validations.NombreValidation;
import com.peryloth.usecase.registry_user.command_queue.validations.SalarioBaseValidation;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigInteger;


@RequiredArgsConstructor
public class RegistryUserUseCase implements IRegistryUserUseCase {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Override
    public Mono<Usuario> registryUserAdmin(Usuario usuario) {
        UsuarioValidationQueue validationQueue = new UsuarioValidationQueue()
                .addValidation(new NombreValidation())
                .addValidation(new ApellidoValidation())
                .addValidation(new EmailValidation(usuarioRepository))
                .addValidation(new SalarioBaseValidation());
        //TODO falta agregar validacion de documento de identidad que no exista ya en db

        BigInteger rolIdFijo = BigInteger.ONE;

        return validationQueue.validate(usuario)
                .then(rolRepository.getRolById(rolIdFijo)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Rol fijo no encontrado")))
                        .flatMap(rol -> {
                            Usuario usuarioConRol = usuario.toBuilder().rol(rol).build();
                            return usuarioRepository.saveUsuario(usuarioConRol);
                        }).log()
                );
    }

    @Override
    public Mono<Usuario> registryNormalUser(Usuario usuario) {
        UsuarioValidationQueue validationQueue = new UsuarioValidationQueue()
                .addValidation(new NombreValidation())
                .addValidation(new ApellidoValidation())
                .addValidation(new EmailValidation(usuarioRepository))
                .addValidation(new SalarioBaseValidation());

        BigInteger rolIdFijo = BigInteger.TWO;

        return validationQueue.validate(usuario)
                .then(rolRepository.getRolById(rolIdFijo)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Rol fijo no encontrado")))
                        .flatMap(rol -> {
                            Usuario usuarioConRol = usuario.toBuilder().rol(rol).build();
                            return usuarioRepository.saveUsuario(usuarioConRol);
                        }).log()
                );
    }
}
