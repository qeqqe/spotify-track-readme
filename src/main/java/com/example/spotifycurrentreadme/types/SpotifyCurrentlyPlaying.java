package com.example.spotifycurrentreadme.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyCurrentlyPlaying(Track item) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Track(
            String id,
            String uri,
            Album album
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(
            String uri,
            List<Image> images
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(String url) {}
}