package io.github.hyperf0rm.deep_vibe.service;


import io.github.hyperf0rm.deep_vibe.dto.AnalyzeAudioTask;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.enums.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AnalyzeAudioTaskProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TrackRepository trackRepository;
    private static final String QUEUE_NAME = "analyze_audio_task_queue";

    public AnalyzeAudioTaskProducer(
            RedisTemplate<String, Object> redisTemplate,
            TrackRepository trackRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.trackRepository = trackRepository;
    }

    @Scheduled(fixedDelay = 1000L)
    public void sendTaskToQueue() {

        List<Track> tracks = trackRepository.findByPreviewUrlIsNotNullAndStatus(TrackQueueStatus.NEW);

        if (tracks == null || tracks.isEmpty()) {
            return;
        }

        for (Track track : tracks) {
            AnalyzeAudioTask task = new AnalyzeAudioTask(track.getId(), track.getPreviewUrl());
            redisTemplate.opsForList().leftPush(QUEUE_NAME, task);
            log.info("Sent task to queue: {}", task);
            track.setStatus(TrackQueueStatus.QUEUED);
        }

        trackRepository.saveAll(tracks);
    }
}
