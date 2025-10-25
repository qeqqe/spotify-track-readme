package com.example.spotifycurrentreadme.types;

public record CurrentPlayingRes(
        String id,
        String imageUrl,
        String trackUri,
        String albumUri
) {}