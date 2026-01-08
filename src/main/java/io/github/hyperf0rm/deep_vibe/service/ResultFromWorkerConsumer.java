package io.github.hyperf0rm.deep_vibe.service;


import io.github.hyperf0rm.deep_vibe.dto.ResultFromWorker;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ResultFromWorkerConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TrackRepository trackRepository;
    private static final String QUEUE_NAME = "results_queue";

    public ResultFromWorkerConsumer(
            RedisTemplate<String, Object> redisTemplate,
            TrackRepository trackRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.trackRepository = trackRepository;
    }

    @Scheduled(fixedDelay = 1000L)
    public void getResultFromQueue() {

        if (queueIsNotEmpty()) {
            Object obj = redisTemplate.opsForList().rightPop(QUEUE_NAME);
            ResultFromWorker result = (ResultFromWorker) obj;
            System.out.printf(result.toString());
            Optional<Track> trackOptional = trackRepository.findById(result.id());
            Track track = trackOptional.orElseThrow();
            track.setBpm(result.bpm());
            trackRepository.save(track);
        }
    }

    public boolean queueIsNotEmpty() {
        Long queueSize = redisTemplate.opsForList().size(QUEUE_NAME);
        return queueSize != null && queueSize > 0;
    }
}
