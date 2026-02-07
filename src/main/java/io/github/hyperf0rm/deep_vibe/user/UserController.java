package io.github.hyperf0rm.deep_vibe.user;

import io.github.hyperf0rm.deep_vibe.analytics.GeneralAnalyticsResponse;
import io.github.hyperf0rm.deep_vibe.analytics.TimelineResponse;
import io.github.hyperf0rm.deep_vibe.analytics.TrackResponse;
import io.github.hyperf0rm.deep_vibe.exception.ExternalAPIException;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.analytics.AnalyticsService;
import io.github.hyperf0rm.deep_vibe.music.service.LastFmService;
import io.github.hyperf0rm.deep_vibe.music.service.PreviewUrlsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        lastFmService.synchronizeUser(username, timestampFrom, timestampTo, stopSignal,
                () -> activeSyncs.remove(username));
        return ResponseEntity.ok("Sync started!");
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
