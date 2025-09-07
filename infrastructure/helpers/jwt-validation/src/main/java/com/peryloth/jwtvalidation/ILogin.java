package com.peryloth.jwtvalidation;

import reactor.core.publisher.Mono;

public interface ILogin {
    Mono<String> login(String email, String password);
}
