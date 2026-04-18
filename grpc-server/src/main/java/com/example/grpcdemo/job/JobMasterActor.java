package com.example.grpcdemo.job;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent of {@link JobWorkerActor}. Relays progress to {@link JobManagerActor} and handles early termination.
 */
public final class JobMasterActor extends AbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(JobMasterActor.class);

    public static Props props(String name, long targetCount, ActorRef jobManager) {
        return Props.create(JobMasterActor.class, name, targetCount, jobManager);
    }

    private final String name;
    private final long targetCount;
    private final ActorRef jobManager;

    private ActorRef worker;
    private long lastCurrent;
    private JobStatus lifecycleStatus = JobStatus.JOB_STATUS_RUNNING;

    private JobMasterActor(String name, long targetCount, ActorRef jobManager) {
        this.name = name;
        this.targetCount = targetCount;
        this.jobManager = jobManager;
    }

    @Override
    public void preStart() {
        LOG.info("master starting job={} targetCount={} path={}", name, targetCount, getSelf().path());
        worker = getContext().actorOf(JobWorkerActor.props(name, targetCount, getSelf()), "worker");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JobWorkerActor.Progress.class, this::onProgress)
                .match(JobWorkerActor.WorkerDone.class, this::onWorkerDone)
                .match(TerminateEarly.class, this::onTerminateEarly)
                .build();
    }

    private void onProgress(JobWorkerActor.Progress p) {
        if (lifecycleStatus != JobStatus.JOB_STATUS_RUNNING) {
            return;
        }
        lastCurrent = p.value;
        LOG.trace("job={} progress {}/{}", name, lastCurrent, targetCount);
        jobManager.tell(
                new JobManagerActor.StateUpdate(name, lastCurrent, JobStatus.JOB_STATUS_RUNNING),
                self());
    }

    private void onWorkerDone(JobWorkerActor.WorkerDone d) {
        if (lifecycleStatus != JobStatus.JOB_STATUS_RUNNING) {
            return;
        }
        lifecycleStatus = JobStatus.JOB_STATUS_DONE;
        LOG.info("job={} worker finished normally (target={})", name, targetCount);
        jobManager.tell(
                new JobManagerActor.StateUpdate(name, targetCount, JobStatus.JOB_STATUS_DONE),
                self());
    }

    private void onTerminateEarly(TerminateEarly t) {
        if (lifecycleStatus != JobStatus.JOB_STATUS_RUNNING) {
            return;
        }
        LOG.info("job={} terminate early at progress {}/{}", name, lastCurrent, targetCount);
        if (worker != null) {
            getContext().stop(worker);
            worker = null;
        }
        lifecycleStatus = JobStatus.JOB_STATUS_TERMINATED;
        jobManager.tell(
                new JobManagerActor.StateUpdate(name, lastCurrent, JobStatus.JOB_STATUS_TERMINATED),
                self());
    }

    /** Stop the worker and mark the job terminated (from API). */
    public static final class TerminateEarly {}
}
