package com.example.grpcdemo.job;

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * gRPC facade over {@link JobManagerActor}.
 */
public final class JobGrpcEndpoint implements JobService {

    private static final Logger LOG = LoggerFactory.getLogger(JobGrpcEndpoint.class);

    private final ActorRef jobManager;
    private final Timeout timeout = Timeout.create(Duration.ofSeconds(10));

    public JobGrpcEndpoint(ActorRef jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public CompletionStage<StartJobResponse> startJob(StartJobRequest in) {
        LOG.info("gRPC StartJob name={} count={}", in.getName(), in.getCount());
        return PatternsCS.ask(
                        jobManager,
                        new JobManagerActor.StartJobCmd(in.getName(), in.getCount()),
                        timeout)
                .thenApply(o -> (StartJobResponse) o);
    }

    @Override
    public CompletionStage<ListJobsResponse> listJobs(ListJobsRequest in) {
        LOG.debug("gRPC ListJobs");
        return PatternsCS.ask(jobManager, new JobManagerActor.ListJobsCmd(), timeout)
                .thenApply(o -> (ListJobsResponse) o);
    }

    @Override
    public CompletionStage<PeekJobResponse> peekJob(PeekJobRequest in) {
        LOG.debug("gRPC PeekJob name={}", in.getName());
        return PatternsCS.ask(
                        jobManager,
                        new JobManagerActor.PeekJobCmd(in.getName()),
                        timeout)
                .thenApply(o -> (PeekJobResponse) o);
    }

    @Override
    public CompletionStage<TerminateJobResponse> terminateJob(TerminateJobRequest in) {
        LOG.info("gRPC TerminateJob name={}", in.getName());
        return PatternsCS.ask(
                        jobManager,
                        new JobManagerActor.TerminateJobCmd(in.getName()),
                        timeout)
                .thenApply(o -> (TerminateJobResponse) o);
    }

    @Override
    public CompletionStage<CleanJobResponse> cleanJob(CleanJobRequest in) {
        LOG.info("gRPC CleanJob name={}", in.getName());
        return PatternsCS.ask(
                        jobManager,
                        new JobManagerActor.CleanJobCmd(in.getName()),
                        timeout)
                .thenApply(o -> (CleanJobResponse) o);
    }
}
