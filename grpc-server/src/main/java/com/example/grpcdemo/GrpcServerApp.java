package com.example.grpcdemo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.example.grpcdemo.actor.CounterActor;
import com.example.grpcdemo.counter.CounterBridge;
import com.example.grpcdemo.grpc.GrpcServerLifecycle;
import com.example.grpcdemo.grpc.PingPongGrpcEndpoint;
import com.example.grpcdemo.job.JobGrpcEndpoint;
import com.example.grpcdemo.job.JobManagerActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone Akka gRPC process (no REST). REST is served by the {@code bff} Micronaut app.
 */
public final class GrpcServerApp {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerApp.class);

    public static void main(String[] args) {
        Config root = ConfigFactory.load();
        String host = root.getString("grpc.host");
        int port = root.getInt("grpc.port");
        LOG.info("Starting grpc-server (Akka gRPC) on {}:{}", host, port);

        Config akkaConf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
                .withFallback(root);
        ActorSystem system = ActorSystem.create("grpcdemo", akkaConf);
        ActorRef counter = system.actorOf(CounterActor.props(), "counter");
        CounterBridge bridge = new CounterBridge(counter);
        PingPongGrpcEndpoint pingPong = new PingPongGrpcEndpoint(bridge);

        ActorRef jobManager = system.actorOf(JobManagerActor.props(), "job-manager");
        JobGrpcEndpoint jobs = new JobGrpcEndpoint(jobManager);

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(system, pingPong, jobs, host, port);
        lifecycle.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> lifecycle.stop()));
    }

    private GrpcServerApp() {}
}
