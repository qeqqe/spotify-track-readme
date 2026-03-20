package com.example.currenttrackreadme.services;

import com.example.currenttrackreadme.types.CurrentPlayingRes;
import com.example.currenttrackreadme.types.FmCurrentPlaying;
import com.example.currenttrackreadme.types.FmTrackInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class TrackService {
  @Value("${last.fm.api.key}")
  private String apiKey;

  @Value("${last.fm.username}")
  private String username;

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public TrackService() {
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
      }
    } catch (Exception e) {
      throw new RuntimeException("Error fetching track info: " + e.getMessage(), e);
    }
    return null;
  }

  public CurrentPlayingRes getCurrentTrack() {
    final int MAX_RETRIES = 2;

    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {

        URI recentTracksUri = URI.create(String.format(
            "https://ws.audioscrobbler.com/2.0/?method=user.getRecentTracks&user=%s&api_key=%s&format=json&limit=1",
            this.username, this.apiKey));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(recentTracksUri)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString());

        // wait and retry
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

        FmCurrentPlaying data = objectMapper.readValue(
            response.body(),
            FmCurrentPlaying.class);

        if (data.recentTracks() == null) {
          return null;
        }

        FmCurrentPlaying.Track track = data.recentTracks().track().get(0);

        String artist = track.artist().name();
        String trackName = track.name();
        FmTrackInfo info = this.getTrackInfo(trackName, artist);

        String coverUrl = null;
        if (info != null && info.track() != null
            && info.track().album() != null
            && info.track().album().coverUrl() != null
            && !info.track().album().coverUrl().isBlank()) {
          coverUrl = info.track().album().coverUrl();
        } else {
          coverUrl = track.coverUrl();
        }
        List<String> tags = (info != null
            && info.track() != null
            && info.track().toptags() != null
            && info.track().toptags().tag() != null)
                ? info.track().toptags().tag().stream().map(FmTrackInfo.Tag::name).toList()
                : List.of();

        Long duration = info.track().durationMs();

        if (duration == 0L) {
          duration = 210000L;
        }

        return new CurrentPlayingRes(
            artist,
            trackName,
            coverUrl,
            track.isNowPlaying(),
            info.track().durationMs(),
            tags);
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

  private FmTrackInfo getTrackInfo(String trackName, String artistName) throws IOException, InterruptedException {
    FmTrackInfo info = null;

    for (int attempt = 0; attempt < 2; ++attempt) {
      try {
        String url = String.format(
            "https://ws.audioscrobbler.com/2.0/?method=track.getInfo&artist=%s&track=%s&api_key=%s&format=json",
            URLEncoder.encode(artistName, StandardCharsets.UTF_8),
            URLEncoder.encode(trackName, StandardCharsets.UTF_8),
            this.apiKey);
        URI uri = URI.create(url);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // wait and retry
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

        info = objectMapper.readValue(response.body(), FmTrackInfo.class);
        break;
      } catch (IOException | InterruptedException e) {
        System.err.println("Error fetching current track (attempt " + (attempt + 1) + "): " + e.getMessage());
        if (attempt == 1) {
          return null;
        }
      } catch (Exception e) {
        System.err.println("Unexpected error: " + e.getMessage());
        return null;
      }
    }
    return info;
  }
}
