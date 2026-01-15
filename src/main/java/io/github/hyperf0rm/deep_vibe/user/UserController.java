package io.github.hyperf0rm.deep_vibe.user;

import io.github.hyperf0rm.deep_vibe.music.dto.AverageBpmResponse;
import io.github.hyperf0rm.deep_vibe.music.dto.LastFmResponse;
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

import java.util.List;

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
    public List<LastFmResponse.Track> doSync(@PathVariable String username) {
        return lastFmService.getRecentTracks(username);
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
    public AverageBpmResponse analyzeUserScrobbles(@PathVariable String username) {
        return analyticsService.calculateAverageBpm(username);
    }
}
