package com.example.grpcdemo.cli;

import com.example.grpcdemo.grpc.PingPongServiceGrpc;
import com.example.grpcdemo.grpc.PingRequest;
import com.example.grpcdemo.grpc.PeekRequest;
import com.example.grpcdemo.grpc.PongResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Minimal gRPC CLI: {@code peek} calls Peek; anything else (or no args) calls Ping.
 */
public final class PingPongCli {

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("grpc.host", "127.0.0.1");
        int port = Integer.getInteger("grpc.port", 9090);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            PingPongServiceGrpc.PingPongServiceBlockingStub stub =
                    PingPongServiceGrpc.newBlockingStub(channel);
            boolean peek = args.length > 0 && "peek".equalsIgnoreCase(args[0]);
            if (peek) {
                PongResponse r = stub.peek(PeekRequest.newBuilder().build());
                System.out.println("peek -> counter=" + r.getCounter());
            } else {
                PongResponse r = stub.ping(PingRequest.newBuilder().build());
                System.out.println("ping -> counter=" + r.getCounter());
            }
        } finally {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private PingPongCli() {}
}
