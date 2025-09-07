package com.peryloth.usecase.validationclient;

import com.peryloth.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ValidationClientUseCase implements IValidationClientUseCase {
    private final UsuarioRepository usuarioRepository;

    @Override
    public Mono<Boolean> isUserValid(String document, String email) {
        return usuarioRepository.getUsuarioByEmailAndDocument(email, document)
                .flatMap(usuario ->
                        Mono.just(usuario.getDocumentoIdentidad().equalsIgnoreCase(document))
                ).defaultIfEmpty(false);
    }
}
