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
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

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
                                Long timestampTo,
                                AtomicBoolean stopSignal,
                                Runnable onComplete) {

        User user = userRepository.findByLastfmUsernameIgnoreCase(username);
        user.setLastSync(Instant.now());
        userRepository.save(user);
        int currentPage = 1;
        int totalPages = 1;

        try {
            do {
                if (stopSignal.get()) {
                    log.warn("Sync interrupted for user: {}", username);
                    return;
                }
                if (currentPage > 1) {
                    Thread.sleep(250L);
                }
                LastFmResponse response = makeRequestToLastFm(username, currentPage, timestampFrom, timestampTo);
                addTracksAndScrobblesFromPage(response, user);
                totalPages = Integer.parseInt(response.recenttracks().attr().totalPages());
                currentPage++;
            } while (currentPage <= totalPages);
        } catch (ExternalAPIException e) {
            log.error("API Error on page {} for user '{}': {}", currentPage, username, e.getMessage());
            if (e.getStatusCode() == 429 || e.getStatusCode() == 403 || e.getStatusCode() == 401) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sync interrupted for user: {}", username);
            return;
        } catch (Exception e) {
            log.error("Unexpected error during sync for user '{}': {}", username, e.getMessage());
        } finally {
            onComplete.run();
        }
        log.info("Sync finished for user: {}", username);
    }

    public LastFmResponse makeRequestToLastFm(String username,
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
                        return body;
                    } else if (statusCode.value() == 404) {
                        throw new UserNotFoundException("User '" + username + "' not found on Last.fm", username);
                    } else if (statusCode.value() == 429) {
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
                    boolean exists = scrobbleRepository.existsByUserAndTrackAndPlayedAt(user, trackEntity, timestamp);
                    if (!exists) {
                        Scrobble newScrobble = new Scrobble();
                        newScrobble.setUser(user);
                        newScrobble.setTrack(trackEntity);
                        newScrobble.setPlayedAt(timestamp);
                        log.info("New scrobble of track ID: {} for user: {}", trackEntity.getId(), user);
                        scrobbleRepository.save(newScrobble);
                    } else {
                        log.debug("Scrobble already exists for user: {}", user);
                    }
                }
            } catch (Exception e) {
                log.error("Error: {}", e.getMessage());
            }
        }
    }

    public void fetchOrSaveUser(String username) {
        User existingUser = userRepository.findByLastfmUsernameIgnoreCase(username);
        if (existingUser == null) {
            LastFmUserResponse userResponse = findUserOnLastFm(username);
            User newUser = new User();
            newUser.setLastfmUsername(userResponse.user().name());
            userRepository.save(newUser);
            }
    }

    public LastFmUserResponse findUserOnLastFm(String username) {
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
                            return body;
                        } else {
                            throw new ExternalAPIException("Last.fm server error: " + statusCode, statusCode.value());
                        }
                    });
    }
}
