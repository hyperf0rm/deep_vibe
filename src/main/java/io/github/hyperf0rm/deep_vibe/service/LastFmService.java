package io.github.hyperf0rm.deep_vibe.service;

import io.github.hyperf0rm.deep_vibe.dto.LastFmResponse;
import io.github.hyperf0rm.deep_vibe.dto.LastFmUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LastFmService {
    private final RestClient restClient;
    private final String apiKey;

    public LastFmService(@Value("${LASTFM_API_KEY}") String apiKey) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
    }

    public LastFmUser getUserInfo(String username) {
        LastFmResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("ws.audioscrobbler.com")
                        .path("/2.0/")
                        .queryParam("method", "user.getinfo")
                        .queryParam("user", username)
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + username + " not found on Last.fm");
                })
                .body(LastFmResponse.class);

        if (response != null && response.user() != null) {
            return response.user();
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User found, but data is empty");
    }
}
