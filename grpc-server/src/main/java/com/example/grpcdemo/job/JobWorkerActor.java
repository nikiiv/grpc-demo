package com.example.grpcdemo.job;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Increments a counter once per second until {@code targetCount} is reached, reporting each value to the master.
 */
public final class JobWorkerActor extends AbstractActorWithTimers {

    private static final Logger LOG = LoggerFactory.getLogger(JobWorkerActor.class);

    public static Props props(String jobName, long targetCount, ActorRef master) {
        return Props.create(JobWorkerActor.class, jobName, targetCount, master);
    }

    private final String jobName;
    private final long targetCount;
    private final ActorRef master;

    private long current;

    private JobWorkerActor(String jobName, long targetCount, ActorRef master) {
        this.jobName = jobName;
        this.targetCount = targetCount;
        this.master = master;
    }

    @Override
    public void preStart() {
        LOG.info("worker started job={} targetCount={}", jobName, targetCount);
        getTimers()
                .startTimerWithFixedDelay(
                        "tick",
                        Tick.INSTANCE,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals(Tick.INSTANCE, t -> onTick()).build();
    }

    private void onTick() {
        current++;
        master.tell(new Progress(current), self());
        if (current >= targetCount) {
            LOG.info("worker job={} reached target {} (stopping)", jobName, targetCount);
            getTimers().cancelAll();
            master.tell(new WorkerDone(), self());
            getContext().stop(self());
        }
    }

    private enum Tick {
        INSTANCE
    }

    static final class Progress {
        final long value;

        Progress(long value) {
            this.value = value;
        }
    }

    /** Terminal message before the worker stops. */
    public static final class WorkerDone {}
}
