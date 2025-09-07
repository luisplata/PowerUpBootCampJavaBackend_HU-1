package com.peryloth.jwtvalidation.login;

import reactor.core.publisher.Mono;

public interface IJwtTokenProvider {
    Mono<String> createToken(String email);

    Mono<String> getUsernameFromToken(String token);
}
