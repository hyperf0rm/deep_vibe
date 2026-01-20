package io.github.hyperf0rm.deep_vibe.user;

import io.github.hyperf0rm.deep_vibe.analytics.GeneralAnalyticsResponse;
import io.github.hyperf0rm.deep_vibe.analytics.TimelineResponse;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.analytics.AnalyticsService;
import io.github.hyperf0rm.deep_vibe.music.service.LastFmService;
import io.github.hyperf0rm.deep_vibe.music.service.PreviewUrlsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final LastFmService lastFmService;
    private final AnalyticsService analyticsService;

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
        if (lastFmService.fetchUser(username).isPresent()) {
            lastFmService.synchronizeUser(username, timestampFrom, timestampTo);
            return ResponseEntity.ok("Sync started!");
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User " + username + " not found on Last.fm");
        }
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
}
