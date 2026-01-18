package io.github.hyperf0rm.deep_vibe.music.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PreviewUrlsResponse(String resultCount, List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String artistName, String trackName, String previewUrl) { }

}
