package dev.rickcloudy.restapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class ReactorTest {
    private final Logger log = LogManager.getLogger(ReactorTest.class);

    private static Mono<String> fetchUserData(int userId) {
        if (userId == 3) {
            return Mono.error(new RuntimeException("Service unavailable for user " + userId));
        }
        return Mono.just("User data for user " + userId)
                .delayElement(Duration.ofMillis(500)); // Simulate network delay
    }

    // Simulated fallback service to fetch default user data
    private static Mono<String> fetchDefaultUserData(int userId) {
        return Mono.just("Default user data for user " + userId)
                .delayElement(Duration.ofMillis(300)); // Simulate network delay
    }

    @Test
    void fluxTest() {
        var flux = Flux.range(1, 30)
                .flatMap(count -> {
                    if(count == 10) {
                        log.debug("Aduh error");
                        return Mono.error(new Exception("Anjay"));
                    }
                    return Flux.just(count);
                })
                .onErrorContinue((e, data) -> {
                    log.info("ngentot {}", data);
                })
                .doOnNext(i -> log.info("Process has been started : {}", i));
        flux.subscribe(i -> System.out.println("Consumed ::" + i));
    }
    @Test
    void fluxTestOnErrorResume() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        List<Integer> userIds = Arrays.asList(1, 2, 3, 4, 5);
        var flux = Flux.range(1, 10)
                .flatMap(userId -> fetchUserData(userId)
                                .onErrorResume(error -> {
                                    System.out.println("Error fetching data for user " + userId + ": " + error.getMessage());
                                    return fetchDefaultUserData(userId);
                                })).log();
        flux.subscribe(
                data -> {
                    System.out.println("Received user data: " + data);
                    latch.countDown();
                },
                error -> {
                    System.err.println("Stream failed with error: " + error.getMessage());
                    latch.countDown();
                },
                () -> {
                    System.out.println("Stream completed successfully.");
                    latch.countDown();
                }
        );
        latch.await();
    }

    @Test
    void fluxGenerate() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    System.out.println("Sink " + sink.toString());
                    System.out.println("State " + state);
                    if(state == 10) sink.complete();
                    return state + 1;
                }
        );
        flux.subscribe(i -> System.out.println("Consumed " + i));
    }

    private Mono<Void> except(int number) {
        if(number == 4) {
            return Mono.error(new Exception("Error"));
        }
        return null;
    }
}
