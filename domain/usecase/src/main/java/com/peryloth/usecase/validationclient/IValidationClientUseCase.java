package com.peryloth.usecase.validationclient;

import reactor.core.publisher.Mono;

public interface IValidationClientUseCase {
    Mono<Boolean> isUserValid(String document, String email);
}
