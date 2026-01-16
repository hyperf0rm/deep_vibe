package io.github.hyperf0rm.deep_vibe.music.service;

import io.github.hyperf0rm.deep_vibe.music.dto.LastFmResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

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

    public List<LastFmResponse.Track> getRecentTracks(String username,
                                                      Long timestampFrom,
                                                      Long timestampTo) {
        LastFmResponse response = makeRequestToLastFm(username, 1, timestampFrom, timestampTo);

        if (response == null) {
            log.error("Last.fm returned null response");
            return List.of();
        }

        if (response.recenttracks() == null) {
            log.error("Last.fm 'recenttracks' field is missing");
            return List.of();
        }

        if (response.recenttracks().track() == null) {
            log.error("Track field is empty");
            return List.of();
        }

        User user = userRepository.findByLastfmUsername(username);

        int totalPages = Integer.parseInt(response.recenttracks().attr().totalPages());
        addTracksAndScrobblesFromPage(response, user);
        if (totalPages > 1) {
            for (int i = 2; i < totalPages + 1; i++) {
                try {
                    LastFmResponse nextResponse = makeRequestToLastFm(username, i, timestampFrom, timestampTo);
                    addTracksAndScrobblesFromPage(nextResponse, user);
                    Thread.sleep(250L);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Error: {}", e.getMessage());
                }
            }
        }

        return response.recenttracks().track();
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
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + username + " not found on Last.fm");
                })
                .body(LastFmResponse.class);
    }

    public void addTracksAndScrobblesFromPage(LastFmResponse response, User user) {
        for (LastFmResponse.Track track : response.recenttracks().track()) {
            try {
                Track trackEntity = trackRepository
                        .findByNameAndArtistName(track.name(), track.artist().name());

                if (trackEntity == null) {
                    Track newTrack = new Track();
                    newTrack.setName(track.name());
                    newTrack.setArtistName(track.artist().name());
                    log.info("New track: {}", newTrack);
                    trackEntity = trackRepository.save(newTrack);
                }

                if (track.date() != null && track.date().uts() != null) {
                    long uts = Long.parseLong(track.date().uts());
                    Instant timestamp = Instant.ofEpochSecond(uts);
                    Scrobble newScrobble = new Scrobble();
                    newScrobble.setUser(user);
                    newScrobble.setTrack(trackEntity);
                    newScrobble.setPlayedAt(timestamp);
                    log.info(newScrobble.toString());
                    scrobbleRepository.save(newScrobble);
                }
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate Track or Scrobble, skipping");
            } catch (Exception e) {
                log.error("Error", e);
            }
        }
    }
}
