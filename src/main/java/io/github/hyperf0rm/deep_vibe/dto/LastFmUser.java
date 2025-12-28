package io.github.hyperf0rm.deep_vibe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastFmUser(String name, String realname, String url, Integer playcount){ }
