package com.peryloth.jwtvalidation;

import reactor.core.publisher.Mono;

public interface IJwtTokenProvider {
    Mono<String> createToken(String email);

    Mono<String> getUsernameFromToken(String token);
}
