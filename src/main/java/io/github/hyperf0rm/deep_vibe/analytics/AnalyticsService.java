package io.github.hyperf0rm.deep_vibe.analytics;


import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.music.repository.ScrobbleRepository;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import io.github.hyperf0rm.deep_vibe.music.repository.TrackRepository;
import io.github.hyperf0rm.deep_vibe.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    public GeneralAnalyticsResponse generalAnalytics(String username, Long timestampFrom, Long timestampTo) {
        User user = userRepository.findByLastfmUsername(username);
        List<Track> tracks;
        if (timestampFrom == null || timestampTo == null) {
            tracks = trackRepository.findByUserScrobblesAndStatus(user, TrackQueueStatus.COMPLETED); // might need to include duplicates
        }
        else {
            Instant from = Instant.ofEpochSecond(timestampFrom);
            Instant to = Instant.ofEpochSecond(timestampTo);
            tracks = trackRepository.findByUserScrobblesAndStatusFilterByPlayedAt(
                    user, TrackQueueStatus.COMPLETED, from, to
            ); // might need to include duplicates
        }

        if  (tracks.isEmpty()) {
            return new GeneralAnalyticsResponse(username, 0, 0F, 0F);
        }
        log.info(tracks.toString());
        int sumBpm = 0;
        float sumRms = 0;
        float sumCentroid = 0;
        int count = 0;
        for (Track track : tracks) {
            sumBpm += track.getBpm();
            sumRms += track.getRms();
            sumCentroid += track.getSpectralCentroid();
            count++;
        }

        int averageBpm = sumBpm / count;
        float averageRms = sumRms / count;
        float averageCentroid = sumCentroid / count;

        return new GeneralAnalyticsResponse(username, averageBpm, averageRms, averageCentroid);

    }


    public List<TimelineResponse> timelineResponse(String username, Long timestampFrom, Long timestampTo) {
        User user = userRepository.findByLastfmUsername(username);

        Instant from = Instant.ofEpochSecond(timestampFrom);
        Instant to = Instant.ofEpochSecond(timestampTo);
        List<Scrobble> scrobbles = scrobbleRepository.findScrobblesByUserFilterByPlayedAt(user, from, to);

        List<LocalDate> dates = new ArrayList<>();
        HashMap<LocalDate, List<Scrobble>> scrobbleDates = new HashMap<>();

        for (Scrobble scrobble : scrobbles) {
            LocalDate scrobbleDate = LocalDate.ofInstant(scrobble.getPlayedAt(), ZoneOffset.UTC);
            if (!scrobbleDates.containsKey(scrobbleDate)) {
                scrobbleDates.put(scrobbleDate, new ArrayList<>());
                dates.add(scrobbleDate);
            }
            List<Scrobble> scrobblesForDate = scrobbleDates.get(scrobbleDate);
            scrobblesForDate.add(scrobble);
        }

        List<TimelineResponse> response = new ArrayList<>();

        for (LocalDate date : dates) {
            List<Scrobble> scrobblesForDate = scrobbleDates.get(date);
            int sumBpm = 0;
            float sumRms = 0;
            float sumCentroid = 0;
            int count = 0;
            for (Scrobble scrobble : scrobblesForDate) {
                Track track = scrobble.getTrack();
                if (!track.getStatus().equals(TrackQueueStatus.COMPLETED)) {
                    continue;
                }
                sumBpm += track.getBpm();
                sumRms += track.getRms();
                sumCentroid += track.getSpectralCentroid();
                count++;
            }
            int averageBpm = sumBpm / count;
            float averageRms = sumRms / count;
            float averageCentroid = sumCentroid / count;
            response.add(new TimelineResponse(username, date, averageBpm, averageRms, averageCentroid));
        }
        Collections.reverse(response);
        return response;

    }

}

