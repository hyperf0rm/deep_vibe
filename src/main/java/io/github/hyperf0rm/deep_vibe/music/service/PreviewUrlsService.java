package io.github.hyperf0rm.deep_vibe.music.service;

import io.github.hyperf0rm.deep_vibe.exception.ExternalAPIException;
import io.github.hyperf0rm.deep_vibe.music.dto.PreviewUrlsResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
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
        List<Track> tracks = trackRepository.findByPreviewUrlIsNullAndStatusNot(TrackQueueStatus.FAILED);

        if (tracks != null && !tracks.isEmpty()) {
            for (Track track : tracks) {
                String trackName = track.getName();
                String artistName = track.getArtistName();
                String fullName = artistName + " " + trackName;
                String[] words = fullName.split(" ");
                String termParam = String.join("+", words);

                log.info("getting url for track: {}; termParam: {}", fullName, termParam);

                try {
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
                            .exchange((request, resp) -> {
                                int statusCode = resp.getStatusCode().value();
                                if (statusCode == 429) {
                                    log.error("Rate limit for preview url service exceeded");
                                    throw new ExternalAPIException("Rate limit for preview url service exceeded", 429);
                                } else if (resp.getStatusCode().is5xxServerError()) {
                                    throw new ExternalAPIException("iTunes Server Error: " + statusCode, statusCode);
                                }
                                else if (resp.getStatusCode().is2xxSuccessful()) {
                                    return resp.bodyTo(PreviewUrlsResponse.class);
                                } else {
                                    return null;
                                }
                            });
                    if (response == null || response.results() == null || response.results().isEmpty()) {
                        log.warn("Not found preview url for track: {}", fullName);
                        track.setStatus(TrackQueueStatus.FAILED);
                        trackRepository.save(track);
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
                    }

                    Thread.sleep(3000);

                    } catch (InterruptedException e) {
                        log.error("Error during thread.sleep", e);
                    } catch (ExternalAPIException e) {
                        log.error("iTunes 429 Rate Limit hit! Stopping batch processing.");
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException err) {
                            log.error("Error during thread.sleep", err);
                        }
                        break;
                    } catch (Exception e) {
                        log.error("Unexpected error for track {}: {}", fullName, e.getMessage());
                    }
            }
        }
    }
}
