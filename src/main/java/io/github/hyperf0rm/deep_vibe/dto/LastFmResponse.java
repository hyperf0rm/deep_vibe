package io.github.hyperf0rm.deep_vibe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastFmResponse(LastFmTracks recenttracks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LastFmTracks(
            List<Track> track,
            @JsonProperty("@attr") Metadata attr
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Track(Artist artist, String name, Date date) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(@JsonProperty("#text") String name) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Date(String uts) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            String totalPages,
            String page,
            String perPage,
            String total
    ) { }
}
