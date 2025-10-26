package com.example.spotifycurrentreadme.services;

import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import com.example.spotifycurrentreadme.types.SpotifyCurrentlyPlaying;
import com.example.spotifycurrentreadme.types.SpotifyRecentTrack;
import com.example.spotifycurrentreadme.types.SpotifyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TrackService {
    @Value("${spotify.sp_dc}")
    private String SP_DC;
    private final SpotifyAuthService spotifyAuthService;
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TrackService(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CurrentPlayingRes getTrackInfo() {
        try {
            CurrentPlayingRes currentTrack = getCurrentTrack();
            if (currentTrack != null) {
                return currentTrack;
            } else {
                return getRecentlyPlayedTrack();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching track info: " + e.getMessage(), e);
        }
    }

    public CurrentPlayingRes getCurrentTrack() {
            final int MAX_RETRIES = 2;
            for(int attempt = 0;attempt < MAX_RETRIES;++attempt){
                try{
                    String accessToken = spotifyAuthService.getToken();
                    if(accessToken == null){
                        throw new RuntimeException("Failed to get access token");
                    }
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                            .GET()
                            .header("Authorization", "Bearer " + accessToken)
                            .header("User-Agent", userAgent)
                            .header("Origin", "https://open.spotify.com")
                            .header("Referer", "https://open.spotify.com")
                            .header("Cookie", "sp_dc=" + SP_DC)
                            .timeout(Duration.ofSeconds(10))
                            .build();

                    HttpResponse<String> response = httpClient
                            .send(request, HttpResponse.BodyHandlers.ofString());

                    if(response.statusCode() == 401) {
                        throw new RuntimeException("Access token expired, retrying with fresh token...");
                    }
                    if (response.statusCode() == 429) {
                        String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
                        int waitTime = Integer.parseInt(retryAfter) * 1000;
                        System.out.println("Rate limited, waiting " + waitTime + "ms...");
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                        continue;
                    }

                    if (response.statusCode() == 204) {
                        return null;
                    }
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to get current track: " + response.statusCode() + " - " + response.body());
                    }

                    SpotifyCurrentlyPlaying data = objectMapper.readValue(
                            response.body(),
                            SpotifyCurrentlyPlaying.class
                    );

                    SpotifyCurrentlyPlaying.Track track = data.item();
                    StringBuilder artistNames = new StringBuilder();
                    for (int i = 0; i < track.album().artists().size(); i++) {
                        artistNames.append(track.album().artists().get(i).name());
                        if (i < track.album().artists().size() - 1) {
                            artistNames.append(", ");
                        }
                    }

                    return new CurrentPlayingRes(
                            track.id(),
                            artistNames.toString(),
                            track.name(),
                            data.progress_ms(),
                            track.duration_ms(),
                            track.album().images().get(1).url(),
                            track.uri(),
                            track.album().uri().split(":")[2],
                            true
                    );
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        return null;
    }

    private CurrentPlayingRes getRecentlyPlayedTrack() {
        final int MAX_RETRIES = 2;
        for(int attempt = 0;attempt < MAX_RETRIES;++attempt){
            try{
                String accessToken = spotifyAuthService.getToken();
                if(accessToken == null){
                    throw new RuntimeException("Failed to get access token");
                }
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.spotify.com/v1/me/player/recently-played?limit=1"))
                        .GET()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("User-Agent", userAgent)
                        .header("Origin", "https://open.spotify.com")
                        .header("Referer", "https://open.spotify.com")
                        .header("Cookie", "sp_dc=" + SP_DC)
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient
                        .send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode() == 401) {
                    throw new RuntimeException("Access token expired, retrying with fresh token...");
                }
                if (response.statusCode() == 429) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
                    int waitTime = Integer.parseInt(retryAfter) * 1000;
                    System.out.println("Rate limited, waiting " + waitTime + "ms...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                    continue;
                }

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to get current track: " + response.statusCode() + " - " + response.body());
                }

                SpotifyResponse data = objectMapper.readValue(response.body(), SpotifyResponse.class);
                SpotifyRecentTrack track = data.items().get(0).track();

                StringBuilder artistNames = new StringBuilder();

                for (int i = 0; i < track.album().artists().size(); i++) {
                    artistNames.append(track.album().artists().get(i).name());
                    if (i < track.album().artists().size() - 1) {
                        artistNames.append(", ");
                    }
                }

                return new CurrentPlayingRes(
                        track.id(),
                        artistNames.toString(),
                        track.name(),
                        0,
                        track.duration_ms(),
                        track.album().images().get(1).url(),
                        track.uri(),
                        track.album().uri().split(":")[2],
                        false
                );
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

}
