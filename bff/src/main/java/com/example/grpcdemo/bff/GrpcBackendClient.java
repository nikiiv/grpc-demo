package com.example.grpcdemo.bff;

import com.example.grpcdemo.job.CleanJobRequest;
import com.example.grpcdemo.job.CleanJobResponse;
import com.example.grpcdemo.job.JobServiceGrpc;
import com.example.grpcdemo.job.ListJobsRequest;
import com.example.grpcdemo.job.ListJobsResponse;
import com.example.grpcdemo.job.PeekJobRequest;
import com.example.grpcdemo.job.PeekJobResponse;
import com.example.grpcdemo.job.StartJobRequest;
import com.example.grpcdemo.job.StartJobResponse;
import com.example.grpcdemo.job.TerminateJobRequest;
import com.example.grpcdemo.job.TerminateJobResponse;
import com.example.grpcdemo.grpc.PingPongServiceGrpc;
import com.example.grpcdemo.grpc.PingRequest;
import com.example.grpcdemo.grpc.PeekRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * gRPC client to the standalone {@code grpc-server} (actor + Akka gRPC).
 */
@Singleton
public final class GrpcBackendClient {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcBackendClient.class);

    private final ManagedChannel channel;
    private final PingPongServiceGrpc.PingPongServiceBlockingStub blocking;
    private final JobServiceGrpc.JobServiceBlockingStub jobs;

    public GrpcBackendClient(
            @Value("${grpc.client.host}") String host,
            @Value("${grpc.client.port}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blocking = PingPongServiceGrpc.newBlockingStub(channel);
        this.jobs = JobServiceGrpc.newBlockingStub(channel);
        LOG.info("gRPC client channel open to {}:{}", host, port);
    }

    public long ping() {
        LOG.debug("upstream PingPong.ping");
        return blocking.ping(PingRequest.newBuilder().build()).getCounter();
    }

    public long peek() {
        LOG.debug("upstream PingPong.peek");
        return blocking.peek(PeekRequest.newBuilder().build()).getCounter();
    }

    public StartJobResponse startJob(String name, long count) {
        LOG.debug("upstream JobService.startJob name={} count={}", name, count);
        return jobs.startJob(
                StartJobRequest.newBuilder().setName(name).setCount(count).build());
    }

    public ListJobsResponse listJobs() {
        LOG.debug("upstream JobService.listJobs");
        return jobs.listJobs(ListJobsRequest.getDefaultInstance());
    }

    public PeekJobResponse peekJob(String name) {
        LOG.debug("upstream JobService.peekJob name={}", name);
        return jobs.peekJob(PeekJobRequest.newBuilder().setName(name).build());
    }

    public TerminateJobResponse terminateJob(String name) {
        LOG.debug("upstream JobService.terminateJob name={}", name);
        return jobs.terminateJob(TerminateJobRequest.newBuilder().setName(name).build());
    }

    public CleanJobResponse cleanJob(String name) {
        LOG.debug("upstream JobService.cleanJob name={}", name);
        return jobs.cleanJob(CleanJobRequest.newBuilder().setName(name).build());
    }

    @PreDestroy
    void close() throws InterruptedException {
        LOG.info("gRPC client channel shutdown");
        channel.shutdown();
        channel.awaitTermination(5, TimeUnit.SECONDS);
    }
}
