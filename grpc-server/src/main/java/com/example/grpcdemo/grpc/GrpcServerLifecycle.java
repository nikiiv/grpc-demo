package com.example.grpcdemo.grpc;

import akka.actor.ActorSystem;
import akka.grpc.javadsl.ServerReflection;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.function.Function;
import com.example.grpcdemo.job.JobService;
import com.example.grpcdemo.job.JobServiceHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Binds the Akka HTTP/2 gRPC endpoint (separate JVM from the Micronaut BFF).
 */
public final class GrpcServerLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final ActorSystem system;
    private final PingPongService pingPong;
    private final JobService jobService;
    private final String host;
    private final int port;

    private volatile ServerBinding binding;

    public GrpcServerLifecycle(
            ActorSystem system,
            PingPongService pingPong,
            JobService jobService,
            String host,
            int port) {
        this.system = system;
        this.pingPong = pingPong;
        this.jobService = jobService;
        this.host = host;
        this.port = port;
    }

    public void start() {
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> pingHandler =
                PingPongServiceHandlerFactory.create(pingPong, system);
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> jobHandler =
                JobServiceHandlerFactory.create(jobService, system);
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> reflection =
                ServerReflection.create(
                        List.of(PingPongService.description, JobService.description), system);
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> serviceHandlers =
                ServiceHandler.concatOrNotFound(pingHandler, jobHandler, reflection);

        Http.get(system)
                .newServerAt(host, port)
                .bind(serviceHandlers)
                .whenComplete((b, err) -> {
                    if (err != null) {
                        LOG.error("gRPC bind failed", err);
                    } else {
                        this.binding = b;
                        LOG.info("gRPC server bound to {}", b.localAddress());
                    }
                });
    }

    public void stop() {
        ServerBinding b = binding;
        if (b != null) {
            LOG.info("Stopping gRPC server (unbind)");
            b.unbind().whenComplete((done, err) -> {
                if (err != null) {
                    LOG.warn("gRPC unbind failed", err);
                }
                system.terminate();
            });
        } else {
            LOG.info("Stopping ActorSystem (no binding)");
            system.terminate();
        }
    }
}
