package org.acme;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.acme.TriggerResource.TriggerState.TriggerRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/triggers")
@ApplicationScoped
public class TriggerResource {
    @ConfigProperty(name = "files.dir", defaultValue = "../files")
    String filesDir;
    @RestClient
    TriggerClient triggerClient;
    @Inject
    Executor executor;

    Map<String, ExpectedFile> expectedFiles;
    private TriggerState state;

    @PostConstruct
    void loadFilesRegistry() throws IOException {
        var files = new Properties();
        try (InputStream is = Files.newInputStream(java.nio.file.Path.of(filesDir, "files.properties"))) {
            files.load(is);
        }
        expectedFiles = files.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> {
            var typeAndLength = e.getValue().toString().split(";");
            return new ExpectedFile(e.getKey().toString(), typeAndLength[0], Integer.parseInt(typeAndLength[1]));
        }));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public TriggerParams trigger(@QueryParam("requests") Integer requests, @QueryParam("minDelayMs") Integer minDelayMs,
            @QueryParam("maxDelayMs") Integer maxDelayMs,
            @QueryParam("usingConcurrencyOf") Integer usingConcurrencyOf) {
        if (state != null && !state.isCompleted()) {
            throw new BadRequestException("Previous trigger not completed yet");
        }
        var params = new TriggerParams(
                requests == null ? 10 : requests,
                Math.min(minDelayMs == null ? 1 : minDelayMs, 1),
                maxDelayMs == null ? 100 : maxDelayMs,
                usingConcurrencyOf == null ? 10 : usingConcurrencyOf);
        state = new TriggerState(params);
        state.start();
        return params;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public TriggerState delete() {
        var rm = state;
        if (state != null) {
            state.stop();
        }
        state = null;
        return rm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TriggerState state() {
        return state;
    }

    @Path("/success")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TriggerRequest> successful() {
        return state == null ? null : state.getSuccessfulRequests();
    }

    @Path("/failed")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TriggerRequest> failed() {
        return state == null ? null : state.getFailedRequests();
    }

    public class TriggerState {
        private final TriggerParams params;
        private final List<TriggerRequest> requests;

        public TriggerState(TriggerParams params) {
            this.params = params;
            var random = new Random();
            var files = expectedFiles.values().stream().toList();
            this.requests = IntStream.range(0, params.requests())
                    .mapToObj(i -> {
                        var xpected = files.get(random.nextInt(files.size() - 1));
                        var delay = random.nextInt(params.minDelayMs(), params.maxDelayMs() + 1);
                        return new TriggerRequest(delay, xpected);
                    })
                    .toList();
        }

        public void start() {
            var queue = requests.stream().map(r -> Uni.createFrom()
                    .item(r)
                    .runSubscriptionOn(executor)
                    .onItem().delayIt().by(Duration.ofMillis(r.getDelay()))
                    .onItem().invoke(rr -> {
                        var start = System.currentTimeMillis();
                        rr.run();
                        var duration = System.currentTimeMillis() - start;
                        Log.infof(" * %s completed in %d ms: success %s", rr.getExpected().name(), duration,
                                rr.isSuccess());
                    }))
                    .collect(Collectors.toList());
            Uni.combine().all().unis(queue).usingConcurrencyOf(params.usingConcurrencyOf()).discardItems().subscribe()
                    .with(
                            __ -> Log.infof("/// All %d requests completed", requests.size()),
                            failure -> Log.error("!!! Failed to process all requests", failure));
        }

        public void stop() {
        }

        public TriggerParams getParams() {
            return params;
        }

        @JsonIgnore
        public List<TriggerRequest> getSuccessfulRequests() {
            return requests.stream().filter(TriggerRequest::isSuccess).toList();
        }

        @JsonIgnore
        public List<TriggerRequest> getFailedRequests() {
            return requests.stream().filter(Predicate.not(TriggerRequest::isSuccess)).toList();
        }

        public int getPercentCompleted() {
            var completed = requests.stream().filter(TriggerRequest::isCompleted).count();
            return (int) ((completed*100)/requests.size());
        }

        public int getPercentSuccess() {
            var success = requests.stream().filter(TriggerRequest::isSuccess).count();
            return (int) ((success*100)/requests.size());
        }

        public boolean isCompleted() {
            return requests.stream().allMatch(TriggerRequest::isCompleted);
        }

        public boolean isSuccess() {
            return requests.stream().allMatch(TriggerRequest::isSuccess);
        }

        public class TriggerRequest {
            private final int delay;
            private final ExpectedFile expected;
            private ActualFile actual;
            private Exception failed;

            public TriggerRequest(int delay, ExpectedFile expected) {
                this.delay = delay;
                this.expected = expected;
            }

            public void run() {
                try {
                    actual = triggerClient.download(expected.name());
                } catch (Exception e) {
                    failed = e;
                }
            }

            public int getDelay() {
                return delay;
            }

            public ExpectedFile getExpected() {
                return expected;
            }

            public ActualFile getActual() {
                return actual;
            }

            public String getFailed() {
                return Objects.toString(failed);
            }

            public boolean isCompleted() {
                return actual != null || failed != null;
            }

            public boolean isSuccess() {
                return actual != null && Objects.equals(expected.name(), actual.name())
                        && Objects.equals(expected.type(), actual.type())
                        && expected.size() == actual.receivedBytes();
            }
        }

        public record Stat(int percentCompleted, int percentSuccess) {

        }
    }

}
