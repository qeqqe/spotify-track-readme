package com.example.spotifycurrentreadme.services;

import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import com.example.spotifycurrentreadme.types.SpotifyCurrentlyPlaying;
import com.example.spotifycurrentreadme.types.SpotifyRecentTrack;
import com.example.spotifycurrentreadme.types.SpotifyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TrackService {
    private final SpotifyAuthService spotifyAuthService;
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
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String accessToken = spotifyAuthService.getAccessToken();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                        .GET()
                        .header("Authorization", "Bearer " + accessToken)
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, 
                        HttpResponse.BodyHandlers.ofString()
                );

                // 204 No Content - nothing playing
                if (response.statusCode() == 204) {
                    return null;
                }

                // refresh token and retry once
                if (response.statusCode() == 401) {
                    System.out.println("Got 401, clearing cache and retrying...");
                    spotifyAuthService.clearCache();
                    continue;
                }

                //  wait and retry
                if (response.statusCode() == 429) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
                    int waitTime = Integer.parseInt(retryAfter) * 1000;
                    System.out.println("Rate limited, waiting " + waitTime + "ms...");
                    Thread.sleep(waitTime);
                    continue;
                }

                // non-2xx responses
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    System.err.println("Failed to get current track: " + response.statusCode() + " - " + response.body());
                    return null;
                }

                SpotifyCurrentlyPlaying data = objectMapper.readValue(
                        response.body(),
                        SpotifyCurrentlyPlaying.class
                );

                if (data.item() == null) {
                    return null;
                }

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
                System.err.println("Error fetching current track (attempt " + (attempt + 1) + "): " + e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    return null;
                }
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    private CurrentPlayingRes getRecentlyPlayedTrack() {
        final int MAX_RETRIES = 2;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String accessToken = spotifyAuthService.getAccessToken();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.spotify.com/v1/me/player/recently-played?limit=1"))
                        .GET()
                        .header("Authorization", "Bearer " + accessToken)
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, 
                        HttpResponse.BodyHandlers.ofString()
                );

                // refresh token and retry once
                if (response.statusCode() == 401) {
                    System.out.println("Got 401, clearing cache and retrying...");
                    spotifyAuthService.clearCache();
                    continue;
                }

                if (response.statusCode() == 429) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse("1");
                    int waitTime = Integer.parseInt(retryAfter) * 1000;
                    System.out.println("Rate limited, waiting " + waitTime + "ms...");
                    Thread.sleep(waitTime);
                    continue;
                }

                // handle non-2xx responses
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    System.err.println("Failed to get recently played track: " + response.statusCode() + " - " + response.body());
                    return null;
                }

                // parse successful response
                SpotifyResponse data = objectMapper.readValue(
                        response.body(), 
                        SpotifyResponse.class
                );

                if (data.items() == null || data.items().isEmpty()) {
                    return null;
                }

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
                System.err.println("Error fetching recently played track (attempt " + (attempt + 1) + "): " + e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    return null;
                }
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                return null;
            }
        }
        
        return null;
    }

}
