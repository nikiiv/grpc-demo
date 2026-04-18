package com.example.grpcdemo.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the ping counter. {@link Ping} increments and replies with the new value;
 * {@link Peek} replies with the current value without incrementing.
 */
public final class CounterActor extends AbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(CounterActor.class);

    private long counter;

    public static Props props() {
        return Props.create(CounterActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, p -> {
                    counter++;
                    LOG.debug("ping counter -> {}", counter);
                    getSender().tell(counter, getSelf());
                })
                .match(Peek.class, p -> {
                    LOG.debug("peek counter -> {}", counter);
                    getSender().tell(counter, getSelf());
                })
                .build();
    }

    /** Increment counter and reply with the new value. */
    public static final class Ping {}

    /** Reply with current counter without changing it. */
    public static final class Peek {}
}
