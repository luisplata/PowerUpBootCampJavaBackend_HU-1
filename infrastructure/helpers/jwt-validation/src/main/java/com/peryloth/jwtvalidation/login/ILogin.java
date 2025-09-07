package com.peryloth.jwtvalidation.login;

import reactor.core.publisher.Mono;

public interface ILogin {
    Mono<String> login(String email, String password);
}
