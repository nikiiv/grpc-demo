package com.example.grpcdemo.grpc;

import com.example.grpcdemo.counter.CounterBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

/**
 * gRPC service implementation — delegates to the counter actor via {@link CounterBridge}.
 */
public final class PingPongGrpcEndpoint implements PingPongService {

    private static final Logger LOG = LoggerFactory.getLogger(PingPongGrpcEndpoint.class);

    private final CounterBridge bridge;

    public PingPongGrpcEndpoint(CounterBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public CompletionStage<PongResponse> ping(PingRequest in) {
        return bridge.ping()
                .thenApply(
                        c -> {
                            LOG.debug("gRPC Ping -> counter={}", c);
                            return PongResponse.newBuilder().setCounter(c).build();
                        });
    }

    @Override
    public CompletionStage<PongResponse> peek(PeekRequest in) {
        return bridge.peek()
                .thenApply(
                        c -> {
                            LOG.debug("gRPC Peek -> counter={}", c);
                            return PongResponse.newBuilder().setCounter(c).build();
                        });
    }
}
