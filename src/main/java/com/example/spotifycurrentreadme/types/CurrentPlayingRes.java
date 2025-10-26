package com.example.spotifycurrentreadme.types;

public record CurrentPlayingRes(
        String id,
        String artist,
        String name,
        long progressMs,
        long durationMs,
        String imageUrl,
        String trackUri,
        String albumUri,
        boolean isPlaying
) {}