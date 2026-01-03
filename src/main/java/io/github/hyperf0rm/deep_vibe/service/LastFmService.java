package io.github.hyperf0rm.deep_vibe.service;

import io.github.hyperf0rm.deep_vibe.dto.LastFmResponse;
import io.github.hyperf0rm.deep_vibe.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.entity.User;
import io.github.hyperf0rm.deep_vibe.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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

    public List<LastFmResponse.Track> getRecentTracks(String username) {
        LastFmResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("ws.audioscrobbler.com")
                        .path("/2.0/")
                        .queryParam("method", "user.getrecenttracks")
                        .queryParam("limit", "200")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + username + " not found on Last.fm");
                })
                .body(LastFmResponse.class);

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
        for (LastFmResponse.Track track : response.recenttracks().track()) {
            try {
                Track trackEntity = trackRepository
                        .findByNameAndArtistName(track.name(), track.artist().name());

                if (trackEntity == null) {
                    Track newTrack = new Track();
                    newTrack.setName(track.name());
                    newTrack.setArtistName(track.artist().name());
                    log.info("New track: {}", newTrack.toString());
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
                log.warn("track or scrobble alr exists, skipping", e);
            } catch (Exception e) {
                log.error("Error", e);
            }
        }

        return response.recenttracks().track();
    }
}
