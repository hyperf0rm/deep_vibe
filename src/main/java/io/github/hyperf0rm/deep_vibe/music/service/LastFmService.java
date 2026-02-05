package io.github.hyperf0rm.deep_vibe.music.service;

import io.github.hyperf0rm.deep_vibe.exception.ExternalAPIException;
import io.github.hyperf0rm.deep_vibe.exception.UserNotFoundException;
import io.github.hyperf0rm.deep_vibe.music.dto.LastFmResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackProjection;
import io.github.hyperf0rm.deep_vibe.user.LastFmUserResponse;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class LastFmService {
    private final RestClient restClient;
    private final String apiKey;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final ScrobbleRepository scrobbleRepository;

    public LastFmService(@Value("${LASTFM_API_KEY}") String apiKey,
                         UserRepository userRepository,
                         TrackRepository trackRepository,
                         ScrobbleRepository scrobbleRepository) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.scrobbleRepository = scrobbleRepository;
    }

    @Async("synchronizationExecutor")
    public void synchronizeUser(String username,
                                Long timestampFrom,
                                Long timestampTo) {
        Optional<LastFmResponse> responseOpt;
        try {
            responseOpt = makeRequestToLastFm(username, 1, timestampFrom, timestampTo);
        } catch (ExternalAPIException e) {
            log.error("Error trying to synchronize user: {}. Error: {}", username, e.getMessage());
            return;
        }

        if (responseOpt.isEmpty()) {
            log.warn("Last.fm return null response for user: {}", username);
            return;
        }

        LastFmResponse lastFmResponse = responseOpt.get();
        User user = userRepository.findByLastfmUsername(username);
        user.setLastSync(Instant.now());
        addTracksAndScrobblesFromPage(lastFmResponse, user);

        int totalPages = Integer.parseInt(lastFmResponse.recenttracks().attr().totalPages());
        if (totalPages > 1) {
            for (int i = 2; i < totalPages + 1; i++) {
                try {
                    Thread.sleep(250L);
                    Optional<LastFmResponse> nextResponse = makeRequestToLastFm(username, i, timestampFrom, timestampTo);
                    if (nextResponse.isPresent()) {
                        addTracksAndScrobblesFromPage(nextResponse.get(), user);
                    } else {
                        log.warn("Page {} for user '{}' came back empty or null", i, username);
                    }
                } catch (ExternalAPIException e) {
                    log.error("API Error on page {} for user '{}': {}", i, username, e.getMessage());
                    if (e.getStatusCode() == 429 || e.getStatusCode() == 403 || e.getStatusCode() == 401) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Sync interrupted for user: {}", username);
                } catch (Exception e) {
                    log.error("Unexpected error during sync for user '{}': {}", username, e.getMessage());
                }
            }
        }
        log.info("Sync finished for user: {}", username);
    }

    public Optional<LastFmResponse> makeRequestToLastFm(String username,
                                              Integer page,
                                              Long timestampFrom,
                                              Long timestampTo) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("ws.audioscrobbler.com")
                        .path("/2.0/")
                        .queryParam("method", "user.getrecenttracks")
                        .queryParam("limit", "200")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("from", timestampFrom)
                        .queryParam("to", timestampTo)
                        .queryParam("format", "json")
                        .queryParam("page", page)
                        .build())
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    if (statusCode.is2xxSuccessful()) {
                        LastFmResponse body = response.bodyTo(LastFmResponse.class);
                        if (body == null || body.recenttracks() == null) {
                            throw new ExternalAPIException("Last.fm returned empty response", 502);
                        }
                        return Optional.of(body);
                    } else if (statusCode.value() == 404) {
                        throw new UserNotFoundException("User '" + username + "' not found on Last.fm", username);
                    } else if (statusCode.value() == 429) {
                        log.error("Last.fm rate limit exceeded");
                        throw new ExternalAPIException("Rate limit exceeded", 429);
                    } else if (statusCode.is4xxClientError()) {
                        throw new ExternalAPIException("Last.fm API client error: " + statusCode, statusCode.value());
                    } else {
                        throw new ExternalAPIException("Last.fm server error: " + statusCode, statusCode.value());
                    }
                });
    }

    public void addTracksAndScrobblesFromPage(LastFmResponse response, User user) {
        for (LastFmResponse.Track track : response.recenttracks().track()) {
            try {
                TrackProjection trackProjection = trackRepository
                        .findTrackProjectionByNameAndArtistName(track.name(), track.artist().name()).orElse(null);
                Track trackEntity;

                if (trackProjection == null) {
                    Track newTrack = new Track();
                    newTrack.setName(track.name());
                    newTrack.setArtistName(track.artist().name());
                    log.info("New track: {}", newTrack);
                    trackEntity = trackRepository.save(newTrack);
                } else {
                    trackEntity = trackRepository.getReferenceById(trackProjection.getId());
                }

                if (track.date() != null && track.date().uts() != null) {
                    long uts = Long.parseLong(track.date().uts());
                    Instant timestamp = Instant.ofEpochSecond(uts);
                    Scrobble newScrobble = new Scrobble();
                    newScrobble.setUser(user);
                    newScrobble.setTrack(trackEntity);
                    newScrobble.setPlayedAt(timestamp);
                    log.info("New scrobble of track ID: {} for user: {}", trackEntity.getId(), user);
                    scrobbleRepository.save(newScrobble);
                }
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate Track or Scrobble, skipping");
            } catch (Exception e) {
                log.error("Failed to process track/scrobble from Last.fm page", e);
            }
        }
    }

    public void fetchOrSaveUser(String username) {
        User existingUser =  userRepository.findByLastfmUsername(username);
        if (existingUser == null) {
            Optional<LastFmUserResponse> userResponse = findUserOnLastFm(username);
            User newUser = new User();
            newUser.setLastfmUsername(userResponse.get().user().name());
            userRepository.save(newUser);
            }
    }

    public Optional<LastFmUserResponse> findUserOnLastFm(String username) {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("http")
                            .host("ws.audioscrobbler.com")
                            .path("/2.0/")
                            .queryParam("method", "user.getinfo")
                            .queryParam("user", username)
                            .queryParam("api_key", apiKey)
                            .queryParam("format", "json")
                            .build())
                    .exchange((request, response) -> {
                        HttpStatusCode statusCode = response.getStatusCode();
                        if (statusCode.value() == 404) {
                            throw new UserNotFoundException("User '" + username + "' not found on Last.fm", username);
                        } else if (statusCode.value() == 429) {
                            throw new ExternalAPIException("Rate limit exceeded", 429);
                        } else if (statusCode.is4xxClientError()) {
                            throw new ExternalAPIException("Last.fm API client error: " + statusCode, statusCode.value());
                        } else if (response.getStatusCode().is2xxSuccessful()) {
                            LastFmUserResponse body = response.bodyTo(LastFmUserResponse.class);
                            if (body == null) {
                                throw new ExternalAPIException("Last.fm returned empty response", 502);
                            }
                            return Optional.of(body);
                        } else {
                            throw new ExternalAPIException("Last.fm server error: " + statusCode, statusCode.value());
                        }
                    });
    }
}
