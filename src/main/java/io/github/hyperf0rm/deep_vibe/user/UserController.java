package io.github.hyperf0rm.deep_vibe.user;

import io.github.hyperf0rm.deep_vibe.analytics.GeneralAnalyticsResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.analytics.AnalyticsService;
import io.github.hyperf0rm.deep_vibe.music.service.LastFmService;
import io.github.hyperf0rm.deep_vibe.music.service.PreviewUrlsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final UserRepository userRepository;
    private final LastFmService lastFmService;
    private final TrackRepository trackRepository;
    private final ScrobbleRepository scrobbleRepository;
    private final PreviewUrlsService previewUrlsService;
    private final AnalyticsService analyticsService;

    public UserController(
            UserRepository userRepository,
            LastFmService service,
            TrackRepository trackRepository,
            ScrobbleRepository scrobbleRepository,
            PreviewUrlsService previewUrlsService,
            AnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.lastFmService = service;
        this.trackRepository = trackRepository;
        this.scrobbleRepository = scrobbleRepository;
        this.previewUrlsService = previewUrlsService;
        this.analyticsService = analyticsService;
    }

    @PostMapping(path = "/add")
    public ResponseEntity<UserResponse> addNewUser(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setLastfmUsername(request.lastfmUsername());
        User savedUser = userRepository.save(user);
        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getLastfmUsername(),
                savedUser.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(path = "/all")
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping(path = "/sync/{username}")
    public ResponseEntity<String> synchronizeUser(@PathVariable String username,
                                                      @RequestParam(name = "from",  required = false) Long timestampFrom,
                                                      @RequestParam(name = "to", required = false) Long timestampTo) {
        lastFmService.synchronizeUser(username, timestampFrom, timestampTo);
        return ResponseEntity.ok("Synchronization started!");
    }

    @GetMapping(path = "/tracks")
    public Iterable<Track> getTracks() {
        return trackRepository.findAll();
    }

    @GetMapping(path = "/scrobbles/{username}")
    public Iterable<Scrobble> getScrobbles(@PathVariable String username) {
        User user = userRepository.findByLastfmUsername(username);
        return scrobbleRepository.findScrobblesByUser(user);
    }

    @GetMapping(path = "/tracks/geturl")
    public void getUrls() {
        previewUrlsService.findPreviewUrls();
    }

    @GetMapping(path = "/analyze/{username}")
    public GeneralAnalyticsResponse analyzeUserScrobbles(
            @PathVariable String username,
            @RequestParam(name = "from",  required = false) Long timestampFrom,
            @RequestParam(name = "to", required = false) Long timestampTo
    ) {
        return analyticsService.GeneralAnalytics(username, timestampFrom, timestampTo);
    }
}
