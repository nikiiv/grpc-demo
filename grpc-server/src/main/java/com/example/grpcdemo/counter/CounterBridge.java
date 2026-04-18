package com.example.grpcdemo.counter;

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import com.example.grpcdemo.actor.CounterActor;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * Ask the counter actor for ping/peek; used by the gRPC service implementation.
 */
public final class CounterBridge {

    private final ActorRef counter;
    private final Timeout timeout =
            Timeout.create(Duration.ofSeconds(5));

    public CounterBridge(ActorRef counter) {
        this.counter = counter;
    }

    public CompletionStage<Long> ping() {
        return PatternsCS.ask(counter, new CounterActor.Ping(), timeout)
                .thenApply(o -> (Long) o);
    }

    public CompletionStage<Long> peek() {
        return PatternsCS.ask(counter, new CounterActor.Peek(), timeout)
                .thenApply(o -> (Long) o);
    }
}
