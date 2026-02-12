package io.github.hyperf0rm.deep_vibe.user;

import io.github.hyperf0rm.deep_vibe.analytics.GeneralAnalyticsResponse;
import io.github.hyperf0rm.deep_vibe.analytics.TimelineResponse;
import io.github.hyperf0rm.deep_vibe.analytics.TrackResponse;
import io.github.hyperf0rm.deep_vibe.analytics.AnalyticsService;
import io.github.hyperf0rm.deep_vibe.music.service.LastFmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(path = "/users")
public class UserController {
    private final LastFmService lastFmService;
    private final AnalyticsService analyticsService;

    private final Map<String, AtomicBoolean> activeSyncs = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public UserController(
            LastFmService lastFmService,
            AnalyticsService analyticsService) {
        this.lastFmService = lastFmService;
        this.analyticsService = analyticsService;
    }

    @GetMapping(path = "/sync/{username}")
    public ResponseEntity<String> synchronizeUser(@PathVariable String username,
                                                      @RequestParam(name = "from",  required = false) Long timestampFrom,
                                                      @RequestParam(name = "to", required = false) Long timestampTo) {
        lastFmService.fetchOrSaveUser(username);
        AtomicBoolean stopSignal = new AtomicBoolean(false);
        activeSyncs.put(username, stopSignal);
        lastFmService.synchronizeUser(
                username,
                timestampFrom,
                timestampTo,
                stopSignal,
                () -> {
            activeSyncs.remove(username);
            SseEmitter e = emitters.get(username);
            if (e != null)  e.complete();
                },
                (progressPercentage) -> {
                    SseEmitter e = emitters.get(username);
                    if (e != null) {
                        try {
                            e.send(progressPercentage);
                        } catch (IOException ex) {
                            emitters.remove(username);
                        }
                    }
                });
        return ResponseEntity.ok("Sync started!");
    }

    @GetMapping(path = "/sync/{username}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSyncProgress(@PathVariable String username) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(username, emitter);
        emitter.onCompletion(() -> emitters.remove(username));
        emitter.onTimeout(() -> {
            log.info("SSE timeout for user: {}", username);
            emitters.remove(username);
        });
        emitter.onError((ex) -> {
            log.error("SSE error for user: {}", username);
            emitters.remove(username);
        });
        return emitter;
    }

    @PostMapping(path = "/sync/{username}/stop")
    public ResponseEntity<String> stopSync(@PathVariable String username) {
        AtomicBoolean stopSignal = activeSyncs.get(username);
        if (stopSignal != null) {
            stopSignal.set(true);
            return ResponseEntity.ok("Sync stopped!");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No active syncs for this user");

    }

    @GetMapping(path = "/analyze/{username}")
    public GeneralAnalyticsResponse analyzeUserScrobbles(
            @PathVariable String username,
            @RequestParam(name = "from",  required = false) Long timestampFrom,
            @RequestParam(name = "to", required = false) Long timestampTo
    ) {
        return analyticsService.generalAnalytics(username, timestampFrom, timestampTo);
    }

    @GetMapping(path = "/timeline/{username}")
    public List<TimelineResponse> timelineAnalytics(
            @PathVariable String username,
            @RequestParam(name = "from",  required = false) Long timestampFrom,
            @RequestParam(name = "to", required = false) Long timestampTo
    ) {
        return analyticsService.timelineResponse(username, timestampFrom, timestampTo);
    }

    @GetMapping(path = "/similar/{id:[0-9]+}")
    public List<TrackResponse> similarTracks(@PathVariable Long id) {
        return analyticsService.findSimilarTracks(id);
    }
}
