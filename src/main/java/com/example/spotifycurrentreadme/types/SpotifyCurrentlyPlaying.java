package com.example.spotifycurrentreadme.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyCurrentlyPlaying(long progress_ms,Track item) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Track(
            String id,
            String uri,
            Album album,
            long duration_ms,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(
            String uri,
            List<Image> images,
            List<Artist> artists
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(String name) {}
}