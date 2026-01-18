package io.github.hyperf0rm.deep_vibe.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastFmUserResponse(LastFmUser user) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LastFmUser(String name) { }

}
