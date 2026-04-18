package com.example.grpcdemo.bff;

import com.example.grpcdemo.job.JobStatus;
import com.example.grpcdemo.job.JobStatusInfo;
import com.example.grpcdemo.job.ListJobsResponse;
import com.example.grpcdemo.job.PeekJobResponse;
import com.example.grpcdemo.job.StartJobResponse;
import com.example.grpcdemo.job.TerminateJobResponse;
import com.example.grpcdemo.job.CleanJobResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for named counter jobs — forwards to the gRPC {@code JobService} on grpc-server.
 */
@Controller("/api/jobs")
public final class JobController {

    private static final Logger LOG = LoggerFactory.getLogger(JobController.class);

    private final GrpcBackendClient backend;

    @Inject
    public JobController(GrpcBackendClient backend) {
        this.backend = backend;
    }

    @Post
    public Map<String, Object> start(@Body StartJobBody body) {
        LOG.info("REST POST /api/jobs name={} count={}", body.name(), body.count());
        StartJobResponse r = backend.startJob(body.name(), body.count());
        return Map.of(
                "ok", r.getOk(),
                "errorMessage", r.getErrorMessage() == null ? "" : r.getErrorMessage());
    }

    @Get
    public Map<String, Object> list() {
        LOG.info("REST GET /api/jobs");
        ListJobsResponse r = backend.listJobs();
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (JobStatusInfo j : r.getJobsList()) {
            jobs.add(toJobMap(j));
        }
        return Map.of("jobs", jobs);
    }

    @Get("/{name}/peek")
    public Map<String, Object> peek(@PathVariable String name) {
        LOG.info("REST GET /api/jobs/{}/peek", name);
        PeekJobResponse r = backend.peekJob(name);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", r.getOk());
        out.put("errorMessage", r.getErrorMessage() == null ? "" : r.getErrorMessage());
        if (r.hasInfo()) {
            out.put("info", toJobMap(r.getInfo()));
        }
        return out;
    }

    @Post("/{name}/terminate")
    public Map<String, Object> terminate(@PathVariable String name) {
        LOG.info("REST POST /api/jobs/{}/terminate", name);
        TerminateJobResponse r = backend.terminateJob(name);
        return Map.of(
                "ok", r.getOk(),
                "errorMessage", r.getErrorMessage() == null ? "" : r.getErrorMessage());
    }

    @Delete("/{name}")
    public Map<String, Object> clean(@PathVariable String name) {
        LOG.info("REST DELETE /api/jobs/{}", name);
        CleanJobResponse r = backend.cleanJob(name);
        return Map.of(
                "ok", r.getOk(),
                "errorMessage", r.getErrorMessage() == null ? "" : r.getErrorMessage());
    }

    private static Map<String, Object> toJobMap(JobStatusInfo i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", i.getName());
        m.put("status", statusToString(i.getStatus()));
        m.put("targetCount", i.getTargetCount());
        m.put("currentCount", i.getCurrentCount());
        return m;
    }

    private static String statusToString(JobStatus s) {
        return switch (s) {
            case JOB_STATUS_RUNNING -> "RUNNING";
            case JOB_STATUS_DONE -> "DONE";
            case JOB_STATUS_TERMINATED -> "TERMINATED";
            default -> "UNKNOWN";
        };
    }

    /** JSON body for {@code POST /api/jobs}. */
    public record StartJobBody(String name, long count) {}
}
