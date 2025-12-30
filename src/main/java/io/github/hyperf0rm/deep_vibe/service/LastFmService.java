package io.github.hyperf0rm.deep_vibe.service;

import io.github.hyperf0rm.deep_vibe.dto.LastFmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class LastFmService {
    private final RestClient restClient;
    private final String apiKey;

    public LastFmService(@Value("${LASTFM_API_KEY}") String apiKey) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
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

        return response.recenttracks().track();
    }
}
