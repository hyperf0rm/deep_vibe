package io.github.hyperf0rm.deep_vibe.user;

public record LastFmUserResponse(LastFmUser user) {

    public record LastFmUser(String name) { }

}
