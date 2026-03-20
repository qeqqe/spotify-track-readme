package com.example.spotifycurrentreadme.types;

import java.util.List;

public record CurrentPlayingRes(
    String artist,
    String name,
    String imageUrl,
    boolean isPlaying,
    // info
    Long duration,
    List<String> tags) {
}
