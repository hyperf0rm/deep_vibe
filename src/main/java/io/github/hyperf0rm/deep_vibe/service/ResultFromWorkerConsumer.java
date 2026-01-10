package io.github.hyperf0rm.deep_vibe.service;


import io.github.hyperf0rm.deep_vibe.dto.ResultFromWorker;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.enums.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
@Slf4j
public class ResultFromWorkerConsumer {

    private final StringRedisTemplate redisTemplate;
    private final TrackRepository trackRepository;
    private final ObjectMapper objectMapper;
    private static final String QUEUE_NAME = "results_queue";

    public ResultFromWorkerConsumer(
            StringRedisTemplate redisTemplate,
            TrackRepository trackRepository,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.trackRepository = trackRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    public void getResultFromQueue() {

        if (queueIsNotEmpty()) {
            String json = redisTemplate.opsForList().rightPop(QUEUE_NAME);
            ResultFromWorker result = objectMapper.readValue(json, ResultFromWorker.class);
            log.info("Consumer got result - id: {}, bpm: {}", result.id(), result.bpm());
            Optional<Track> trackOptional = trackRepository.findById(result.id());
            Track track = trackOptional.orElseThrow();
            track.setBpm(result.bpm());
            track.setStatus(TrackQueueStatus.COMPLETED);
            trackRepository.save(track);
        }
    }

    public boolean queueIsNotEmpty() {
        Long queueSize = redisTemplate.opsForList().size(QUEUE_NAME);
        return queueSize != null && queueSize > 0;
    }
}
