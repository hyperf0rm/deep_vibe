package io.github.hyperf0rm.deep_vibe.music.service;

import io.github.hyperf0rm.deep_vibe.exception.ExternalAPIException;
import io.github.hyperf0rm.deep_vibe.music.dto.PreviewUrlsResponse;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
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
                String trackName = track.getName().toLowerCase();
                String artistName = track.getArtistName().toLowerCase();
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
                        log.warn("Preview url not found for track {} - {}", artistName, trackName);
                        track.setStatus(TrackQueueStatus.FAILED);
                        trackRepository.save(track);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            log.error("Error during thread.sleep", e);
                        }
                    }
                    else {
                        PreviewUrlsResponse.Result bestMatch = null;
                        PreviewUrlsResponse.Result fallbackMatch = null;
                        JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
                        double bestSimilarityScore = -1.0;

                        for (PreviewUrlsResponse.Result result : response.results()) {

                            String itunesArtist = result.artistName().toLowerCase().trim();
                            String itunesTrack = result.trackName().toLowerCase().trim();

                            if (!itunesArtist.contains(artistName)
                                    && !artistName.contains(itunesArtist)) {
                                continue;
                            }

                            if (itunesArtist.equals(artistName)
                                    && itunesTrack.equals(trackName)) {
                                bestMatch = result;
                                break;
                            }

                            double currentSimilarityScore = jaroWinkler.apply(itunesTrack, trackName);
                            if (currentSimilarityScore > bestSimilarityScore) {
                                if (currentSimilarityScore > 0.8 && (itunesTrack.contains(trackName) || trackName.contains(itunesTrack))) {
                                    bestSimilarityScore = currentSimilarityScore;
                                    fallbackMatch = result;
                                }
                            }
                        }

                        PreviewUrlsResponse.Result finalMatch = (bestMatch != null) ? bestMatch : fallbackMatch;
                        if (finalMatch != null) {
                            String previewUrl = finalMatch.previewUrl();
                            log.info("Found preview url for track {} - {} (Type: {}): {}",
                                    finalMatch.artistName(),
                                    finalMatch.trackName(),
                                    (bestMatch != null ? "STRICT" : "SOFT"),
                                    previewUrl);
                            track.setPreviewUrl(previewUrl);
                            trackRepository.save(track);
                        } else {
                            log.warn("Preview url not found for track {} - {}", artistName, trackName);
                            track.setStatus(TrackQueueStatus.FAILED);
                            trackRepository.save(track);
                        }
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
