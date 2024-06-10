package dev.rickcloudy.restapi.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveLogger {
    private static final Logger log = LogManager.getLogger(ReactiveLogger.class);

    public static <T> Mono<T> logMono(Mono<T> mono) {
        return mono.doOnNext(item -> log.info("Next: {}", item))
                .doOnError(error -> log.error("Error: ", error))
                .doOnSuccess(item -> log.info("Complete"));
    }

    public static <T> Flux<T> logFlux(Flux<T> flux) {
        return flux.doOnNext(item -> log.info("Next: {}", item))
                .doOnError(error -> log.error("Error: ", error))
                .doOnComplete(() -> log.info("Complete"));
    }
}
