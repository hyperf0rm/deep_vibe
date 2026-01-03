package io.github.hyperf0rm.deep_vibe.dto;

import java.util.List;

public record PreviewUrlsResponse(String resultCount, List<Result> results) {

    public record Result(String artistName, String trackName, String previewUrl) { }

}
