package io.github.hyperf0rm.deep_vibe.analytics;


import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;


@Service
@Slf4j
public class AnalyticsService {

    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public AnalyticsService(TrackRepository trackRepository,
                            UserRepository userRepository) {
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
    }

    public GeneralAnalyticsResponse GeneralAnalytics(String username, Long timestampFrom, Long timestampTo) {
        User user = userRepository.findByLastfmUsername(username);
        List<Track> tracks;
        if (timestampFrom == null || timestampTo == null) {
            tracks = trackRepository.findByUserScrobblesAndStatus(user, TrackQueueStatus.COMPLETED);
        }
        else {
            Instant from = Instant.ofEpochSecond(timestampFrom);
            Instant to = Instant.ofEpochSecond(timestampTo);
            tracks = trackRepository.findByUserScrobblesAndStatusFilterByPlayedAt(
                    user, TrackQueueStatus.COMPLETED, from, to
            );
        }

        if  (tracks.isEmpty()) {
            return new GeneralAnalyticsResponse(username, 0, 0F);
        }
        log.info(tracks.toString());
        int sumBpm = 0;
        float sumRms = 0;
        int count = 0;
        for (Track track : tracks) {
            sumBpm += track.getBpm();
            sumRms += track.getRms();
            count++;
        }

        int averageBpm = sumBpm / count;
        float averageRms = sumRms / count;

        return new GeneralAnalyticsResponse(username, averageBpm, averageRms);

    }

}

