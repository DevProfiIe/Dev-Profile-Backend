package com.devprofile.DevProfile.config;

import org.springframework.data.domain.Example;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonoConfig {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Example.class.getName());

        Hooks.onErrorDropped(error -> {
            logger.log(Level.WARNING, "Exception happened:", error);
        });

        Mono<String> mono = Mono.fromCallable(() -> {
                    Thread.sleep(1000); // 비동기 작업
                    return "Success";
                })
                .timeout(Duration.ofMillis(500))
                .onErrorResume(throwable -> Mono.just("Timeout"));

        mono.subscribe(result -> {
            logger.info("Result: " + result);
        });
    }
}
