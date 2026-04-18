package com.example.grpcdemo.job;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of named jobs: one {@link JobMasterActor} per job. State is updated from masters via {@link StateUpdate}.
 */
public final class JobManagerActor extends AbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(JobManagerActor.class);

    public static Props props() {
        return Props.create(JobManagerActor.class);
    }

    private final ConcurrentHashMap<String, JobState> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ActorRef> masters = new ConcurrentHashMap<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartJobCmd.class, this::onStartJob)
                .match(ListJobsCmd.class, this::onListJobs)
                .match(PeekJobCmd.class, this::onPeekJob)
                .match(TerminateJobCmd.class, this::onTerminateJob)
                .match(CleanJobCmd.class, this::onCleanJob)
                .match(StateUpdate.class, this::onStateUpdate)
                .build();
    }

    private void onStartJob(StartJobCmd cmd) {
        ActorRef replyTo = getSender();
        String name = cmd.name.trim();
        if (!JobNaming.validName(name)) {
            LOG.warn("StartJob rejected: invalid name '{}'", name);
            replyTo.tell(
                    StartJobResponse.newBuilder()
                            .setOk(false)
                            .setErrorMessage("invalid name: use 1–64 chars [a-zA-Z0-9_-] starting with a letter")
                            .build(),
                    self());
            return;
        }
        if (states.containsKey(name)) {
            LOG.warn("StartJob rejected: duplicate name '{}'", name);
            replyTo.tell(
                    StartJobResponse.newBuilder().setOk(false).setErrorMessage("job name already exists").build(),
                    self());
            return;
        }
        if (cmd.count <= 0 || cmd.count > 1_000_000L) {
            LOG.warn("StartJob rejected: bad count {} for {}", cmd.count, name);
            replyTo.tell(
                    StartJobResponse.newBuilder()
                            .setOk(false)
                            .setErrorMessage("count must be between 1 and 1000000")
                            .build(),
                    self());
            return;
        }
        ActorRef master =
                getContext().actorOf(JobMasterActor.props(name, cmd.count, getSelf()), JobNaming.actorName(name));
        states.put(
                name,
                new JobState(name, cmd.count, 0, JobStatus.JOB_STATUS_RUNNING, master));
        masters.put(name, master);
        LOG.info("job started name={} count={} master={}", name, cmd.count, master.path());
        replyTo.tell(StartJobResponse.newBuilder().setOk(true).build(), self());
    }

    private void onListJobs(ListJobsCmd cmd) {
        ActorRef replyTo = getSender();
        List<JobStatusInfo> list = new ArrayList<>();
        states.values().stream()
                .sorted(Comparator.comparing(s -> s.name))
                .forEach(s -> list.add(toProto(s)));
        LOG.debug("ListJobs -> {} job(s)", list.size());
        replyTo.tell(ListJobsResponse.newBuilder().addAllJobs(list).build(), self());
    }

    private void onPeekJob(PeekJobCmd cmd) {
        ActorRef replyTo = getSender();
        JobState s = states.get(cmd.name);
        if (s == null) {
            LOG.debug("PeekJob: unknown job '{}'", cmd.name);
            replyTo.tell(
                    PeekJobResponse.newBuilder().setOk(false).setErrorMessage("unknown job").build(),
                    self());
            return;
        }
        replyTo.tell(
                PeekJobResponse.newBuilder().setOk(true).setInfo(toProto(s)).build(),
                self());
    }

    private void onTerminateJob(TerminateJobCmd cmd) {
        ActorRef replyTo = getSender();
        JobState s = states.get(cmd.name);
        if (s == null) {
            LOG.warn("TerminateJob: unknown job '{}'", cmd.name);
            replyTo.tell(
                    TerminateJobResponse.newBuilder().setOk(false).setErrorMessage("unknown job").build(),
                    self());
            return;
        }
        if (s.status != JobStatus.JOB_STATUS_RUNNING) {
            LOG.warn("TerminateJob: job '{}' not running (status={})", cmd.name, s.status);
            replyTo.tell(
                    TerminateJobResponse.newBuilder()
                            .setOk(false)
                            .setErrorMessage("job is not running")
                            .build(),
                    self());
            return;
        }
        LOG.info("job terminate requested name={}", cmd.name);
        s.masterRef.tell(new JobMasterActor.TerminateEarly(), self());
        replyTo.tell(TerminateJobResponse.newBuilder().setOk(true).build(), self());
    }

    private void onCleanJob(CleanJobCmd cmd) {
        ActorRef replyTo = getSender();
        JobState s = states.get(cmd.name);
        if (s == null) {
            LOG.warn("CleanJob: unknown job '{}'", cmd.name);
            replyTo.tell(
                    CleanJobResponse.newBuilder().setOk(false).setErrorMessage("unknown job").build(),
                    self());
            return;
        }
        if (s.status != JobStatus.JOB_STATUS_DONE && s.status != JobStatus.JOB_STATUS_TERMINATED) {
            LOG.warn("CleanJob: job '{}' not finished (status={})", cmd.name, s.status);
            replyTo.tell(
                    CleanJobResponse.newBuilder()
                            .setOk(false)
                            .setErrorMessage("job must be finished or terminated before cleanup")
                            .build(),
                    self());
            return;
        }
        LOG.info("job cleaned name={} status={}", cmd.name, s.status);
        getContext().stop(s.masterRef);
        states.remove(cmd.name);
        masters.remove(cmd.name);
        replyTo.tell(CleanJobResponse.newBuilder().setOk(true).build(), self());
    }

    private void onStateUpdate(StateUpdate u) {
        ActorRef master = masters.get(u.name);
        if (master == null || !master.equals(getSender())) {
            return;
        }
        JobState s = states.get(u.name);
        if (s != null) {
            s.currentCount = u.current;
            s.status = u.status;
        }
    }

    private static JobStatusInfo toProto(JobState s) {
        return JobStatusInfo.newBuilder()
                .setName(s.name)
                .setStatus(s.status)
                .setTargetCount(s.targetCount)
                .setCurrentCount(s.currentCount)
                .build();
    }

    public static final class JobState {
        public final String name;
        public final long targetCount;
        public volatile long currentCount;
        public volatile JobStatus status;
        public final ActorRef masterRef;

        JobState(String name, long targetCount, long currentCount, JobStatus status, ActorRef masterRef) {
            this.name = name;
            this.targetCount = targetCount;
            this.currentCount = currentCount;
            this.status = status;
            this.masterRef = masterRef;
        }
    }

    public static final class StartJobCmd {
        public final String name;
        public final long count;

        public StartJobCmd(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    public static final class ListJobsCmd {}

    public static final class PeekJobCmd {
        public final String name;

        public PeekJobCmd(String name) {
            this.name = name;
        }
    }

    public static final class TerminateJobCmd {
        public final String name;

        public TerminateJobCmd(String name) {
            this.name = name;
        }
    }

    public static final class CleanJobCmd {
        public final String name;

        public CleanJobCmd(String name) {
            this.name = name;
        }
    }

    public static final class StateUpdate {
        public final String name;
        public final long current;
        public final JobStatus status;

        public StateUpdate(String name, long current, JobStatus status) {
            this.name = name;
            this.current = current;
            this.status = status;
        }
    }
}
