package com.example.grpcdemo.bff;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST API for the web UI — forwards to the gRPC backend via {@link GrpcBackendClient}.
 */
@Controller("/api")
public final class CounterController {

    private static final Logger LOG = LoggerFactory.getLogger(CounterController.class);

    private final GrpcBackendClient backend;

    @Inject
    public CounterController(GrpcBackendClient backend) {
        this.backend = backend;
    }

    @Get("/ping")
    public Map<String, Long> ping() {
        LOG.info("REST GET /api/ping");
        long c = backend.ping();
        LOG.debug("ping -> counter={}", c);
        return Map.of("counter", c);
    }

    @Get("/peek")
    public Map<String, Long> peek() {
        LOG.info("REST GET /api/peek");
        long c = backend.peek();
        LOG.debug("peek -> counter={}", c);
        return Map.of("counter", c);
    }
}
