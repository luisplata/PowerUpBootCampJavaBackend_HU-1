package com.peryloth.jwtvalidation;

import reactor.core.publisher.Mono;

public interface IValidateJwt {
    Mono<Void> validate(String jwt);
}
