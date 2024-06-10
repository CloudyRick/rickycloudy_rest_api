package dev.rickcloudy.restapi.controller;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Handler {
    Mono<ServerResponse> save(ServerRequest request);
    Mono<ServerResponse> saveAll(ServerRequest request);
    Mono<ServerResponse> findById(ServerRequest request);
    Mono<ServerResponse> update(ServerRequest request);
    Mono<ServerResponse> delete(ServerRequest request);
    Mono<ServerResponse> findAll(ServerRequest request);
    Mono<ServerResponse> findByParams(ServerRequest request);
}
