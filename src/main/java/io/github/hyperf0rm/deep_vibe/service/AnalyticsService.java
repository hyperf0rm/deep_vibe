package io.github.hyperf0rm.deep_vibe.service;


import io.github.hyperf0rm.deep_vibe.dto.AverageBpmResponse;
import io.github.hyperf0rm.deep_vibe.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.entity.User;
import io.github.hyperf0rm.deep_vibe.enums.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class AnalyticsService {

    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final ScrobbleRepository scrobbleRepository;

    public AnalyticsService(TrackRepository trackRepository,
                            UserRepository userRepository,
                            ScrobbleRepository scrobbleRepository) {
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
        this.scrobbleRepository = scrobbleRepository;
    }

    public AverageBpmResponse calculateAverageBpm(String username) {
        User user = userRepository.findByLastfmUsername(username);
        List<Track> tracks = trackRepository.findByUserScrobblesAndStatus(user, TrackQueueStatus.COMPLETED);
        if  (tracks.isEmpty()) {
            return new AverageBpmResponse(username, 0);
        }
        log.info(tracks.toString());
        int sumBpm = 0;
        int count = 0;
        for (Track track : tracks) {
            sumBpm += track.getBpm();
            count++;
        }

        int averageBpm = sumBpm / count;

        return new AverageBpmResponse(username, averageBpm);

    }

}

