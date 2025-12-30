package io.github.hyperf0rm.deep_vibe.service;

import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class PreviewUrlsService {
    private final RestClient restClient;
    private final TrackRepository trackRepository;

    public PreviewUrlsService(TrackRepository trackRepository) {
        this.restClient = RestClient.create();
        this.trackRepository = trackRepository;
    }




}
