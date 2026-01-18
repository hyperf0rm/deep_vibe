package io.github.hyperf0rm.deep_vibe.music.service;

import io.github.hyperf0rm.deep_vibe.music.dto.PreviewUrlsResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class PreviewUrlsService {
    private final RestClient restClient;
    private final TrackRepository trackRepository;

    public PreviewUrlsService(TrackRepository trackRepository) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("text", "javascript")
        ));
        this.restClient = RestClient.builder()
                .messageConverters(converters -> converters.add(converter))
                .build();
        this.trackRepository = trackRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void findPreviewUrls() {
        List<Track> tracks = trackRepository.findByPreviewUrlIsNull();

        if (tracks != null && !tracks.isEmpty()) {
            for (Track track : tracks) {
                String trackName = track.getName();
                String artistName = track.getArtistName();
                String fullName = artistName + " " + trackName;
                String[] words = fullName.split(" ");
                String termParam = String.join("+", words);

                log.info("getting url for track: {}; termParam: {}", fullName, termParam);

                PreviewUrlsResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("itunes.apple.com")
                                .path("/search")
                                .queryParam("term", termParam)
                                .queryParam("media", "music")
                                .queryParam("entity", "song")
                                .queryParam("limit", "200")
                                .build())
                        .retrieve()
                        .body(PreviewUrlsResponse.class);
                if (response == null || response.results() == null || response.results().isEmpty()) {
                    log.warn("Not found preview url for track: {}", fullName);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        log.error("Error during thread.sleep", e);
                    }
                }
                else {
                    String previewUrl = response.results().getFirst().previewUrl();
                    log.info("Found default preview url for track {}", fullName);

                    for (PreviewUrlsResponse.Result result : response.results()) {
                        if (result.artistName().equalsIgnoreCase(artistName)
                                && result.trackName().equalsIgnoreCase(trackName)) {
                            previewUrl = result.previewUrl();
                            log.info("Found preview url for track {} - {}: {}",
                                    result.artistName(), result.trackName(), previewUrl
                            );
                            break;
                        }
                    }
                    track.setPreviewUrl(previewUrl);
                    trackRepository.save(track);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        log.error("Error during thread.sleep", e);
                    }
                }
            }
        }
    }
}
