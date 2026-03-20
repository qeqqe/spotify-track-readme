package com.example.currenttrackreadme.services;

import com.example.currenttrackreadme.types.CurrentPlayingRes;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class SVGService {

  private final TrackService trackService;

  private final HttpClient httpClient;

  public SVGService(TrackService trackService, HttpClient httpClient) {
    this.trackService = trackService;
    this.httpClient = httpClient;
  }

  private record Theme(
      String gradStart,
      String gradEnd,
      String accent,
      String track,
      String textPri,
      String textSec) {
  }

  private static final Theme THEME_RAP = new Theme("#0e0020", "#1e0840", "#c9a227", "#2e1850", "#ffffff", "#c9a227");
  private static final Theme THEME_ROCK = new Theme("#1a0000", "#2d0808", "#e03030", "#3d1010", "#ffffff", "#e07575");
  private static final Theme THEME_POP = new Theme("#25001a", "#1a000f", "#ff6b9d", "#380028", "#ffffff", "#ff9ec4");
  private static final Theme THEME_INDIE = new Theme("#061410", "#0d2420", "#4ecdc4", "#163530", "#ffffff", "#88d8d3");
  private static final Theme THEME_ELECTRONIC = new Theme("#00001f", "#08001a", "#00d4ff", "#0a0a35", "#ffffff",
      "#80eaff");
  private static final Theme THEME_LOVE = new Theme("#1a000e", "#2d0020", "#ff4081", "#3d0030", "#ffffff", "#ff80ab");
  private static final Theme THEME_DEFAULT = new Theme("#141414", "#1e1e1e", "#aaaaaa", "#2e2e2e", "#ffffff",
      "#888888");

  private Theme resolveTheme(List<String> tags) {
    if (tags == null || tags.isEmpty())
      return THEME_DEFAULT;
    for (String raw : tags) {
      String t = raw.toLowerCase();
      if (t.contains("trap") || t.contains("hip-hop") || t.contains("hip hop") || t.contains("rap"))
        return THEME_RAP;
      if (t.contains("rock"))
        return THEME_ROCK;
      if (t.contains("electronic") || t.contains("dance"))
        return THEME_ELECTRONIC;
      if (t.contains("alternative") || t.contains("indie"))
        return THEME_INDIE;
      if (t.contains("pop"))
        return THEME_POP;
      if (t.contains("love") || t.contains("romance") || t.contains("rnb") || t.contains("r&b"))
        return THEME_LOVE;
    }
    return THEME_DEFAULT;
  }

  public String generateSVG() {
    CurrentPlayingRes track = trackService.getTrackInfo();
    if (track == null)
      return errorSVG();
    Theme theme = resolveTheme(track.tags());
    return track.isPlaying()
        ? generateNowPlayingSVG(track, theme)
        : generateRecentlyPlayedSVG(track, theme);
  }

  private String generateNowPlayingSVG(CurrentPlayingRes track, Theme theme) {
    long loopSec = Math.max(track.duration() / 1000, 1);
    String art = getAlbumImageAsBase64(track.imageUrl());

    return """
        <svg width="400" height="120" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
              <stop offset="0%%"   style="stop-color:%s;stop-opacity:1"/>
              <stop offset="100%%" style="stop-color:%s;stop-opacity:1"/>
            </linearGradient>
            <clipPath id="card"><rect width="400" height="120" rx="12"/></clipPath>
            <clipPath id="art" ><rect x="10" y="10" width="100" height="100" rx="8"/></clipPath>
          </defs>
          <style>
            @keyframes progress {
              from { width: 0px; }
              to   { width: 270px; }
            }
            @keyframes pulse {
              0%%,100%% { opacity:1;   r:3.5; }
              50%%      { opacity:0.3; r:5.5; }
            }
            @keyframes eq1 { 0%%,100%% {height:7px; y:95px} 50%% {height:18px;y:84px} }
            @keyframes eq2 { 0%%,100%% {height:14px;y:88px} 50%% {height:7px; y:95px} }
            @keyframes eq3 { 0%%,100%% {height:11px;y:91px} 50%% {height:20px;y:82px} }
            @keyframes eq4 { 0%%,100%% {height:18px;y:84px} 50%% {height:9px; y:93px} }
            @keyframes eq5 { 0%%,100%% {height:9px; y:93px} 50%% {height:15px;y:87px} }
            .prog { animation: progress %ds linear forwards; }
            .dot  { animation: pulse 1.5s ease-in-out infinite; }
            .eq1  { animation: eq1 0.55s ease-in-out infinite; }
            .eq2  { animation: eq2 0.70s ease-in-out infinite; }
            .eq3  { animation: eq3 0.50s ease-in-out infinite; }
            .eq4  { animation: eq4 0.80s ease-in-out infinite; }
            .eq5  { animation: eq5 0.65s ease-in-out infinite; }
          </style>

          <rect width="400" height="120" fill="url(#bg)" clip-path="url(#card)"/>
          <image x="10" y="10" width="100" height="100" href="%s" clip-path="url(#art)"/>

          <text x="120" y="33"
                fill="%s" font-family="Arial,sans-serif" font-size="15" font-weight="bold">
            %s
          </text>
          <text x="120" y="51"
                fill="%s" font-family="Arial,sans-serif" font-size="12">
            %s
          </text>

          <rect x="120" y="78" width="270" height="4" fill="%s" rx="2"/>
          <rect class="prog" x="120" y="78" width="0" height="4" fill="%s" rx="2"/>

          <text x="390" y="96"
                fill="%s" font-family="Arial,sans-serif" font-size="10" text-anchor="end">
            %s
          </text>

          <rect class="eq1" x="126" y="95" width="4" height="7"  fill="%s" rx="1"/>
          <rect class="eq2" x="132" y="88" width="4" height="14" fill="%s" rx="1"/>
          <rect class="eq3" x="138" y="91" width="4" height="11" fill="%s" rx="1"/>
          <rect class="eq4" x="144" y="84" width="4" height="18" fill="%s" rx="1"/>
          <rect class="eq5" x="150" y="93" width="4" height="9"  fill="%s" rx="1"/>

          <circle class="dot" cx="386" cy="20" r="3.5" fill="%s"/>
        </svg>
        """.formatted(
        theme.gradStart(), theme.gradEnd(),
        loopSec,
        art,
        theme.textPri(), truncateText(track.name(), 24),
        theme.textSec(), truncateText(track.artist(), 28),
        theme.track(), theme.accent(),
        theme.textSec(), formatTime(track.duration()),
        theme.accent(), theme.accent(), theme.accent(), theme.accent(), theme.accent(),
        theme.accent());
  }

  private String generateRecentlyPlayedSVG(CurrentPlayingRes track, Theme theme) {
    String art = getAlbumImageAsBase64(track.imageUrl());

    return """
        <svg width="400" height="120" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
              <stop offset="0%%"   style="stop-color:%s;stop-opacity:1"/>
              <stop offset="100%%" style="stop-color:%s;stop-opacity:1"/>
            </linearGradient>
            <clipPath id="card"><rect width="400" height="120" rx="12"/></clipPath>
            <clipPath id="art" ><rect x="10" y="10" width="100" height="100" rx="8"/></clipPath>
          </defs>

          <rect width="400" height="120" fill="url(#bg)" clip-path="url(#card)"/>
          <image x="10" y="10" width="100" height="100"
                 href="%s" clip-path="url(#art)" opacity="0.55"/>

          <text x="120" y="33"
                fill="%s" font-family="Arial,sans-serif" font-size="15" font-weight="600">
            %s
          </text>
          <text x="120" y="51"
                fill="%s" font-family="Arial,sans-serif" font-size="12">
            %s
          </text>
          <text x="120" y="67"
                fill="%s" font-family="Arial,sans-serif" font-size="10" font-style="italic">
            Recently played
          </text>

          <!-- Static full bar (dimmed) -->
          <rect x="120" y="78" width="270" height="3" fill="%s" rx="2"/>
          <rect x="120" y="78" width="270" height="3" fill="%s" rx="2" opacity="0.2"/>

          <text x="120" y="95"
                fill="%s" font-family="Arial,sans-serif" font-size="10">
            0:00
          </text>
          <text x="390" y="95"
                fill="%s" font-family="Arial,sans-serif" font-size="10" text-anchor="end">
            %s
          </text>

          <!-- Inactive dot -->
          <circle cx="390" cy="20" r="3" fill="%s" opacity="0.35"/>
        </svg>
        """.formatted(
        theme.gradStart(), theme.gradEnd(),
        art,
        theme.textSec(), truncateText(track.name(), 24),
        theme.textSec(), truncateText(track.artist(), 28),
        theme.textSec(),
        theme.track(), theme.accent(),
        theme.textSec(), theme.textSec(), formatTime(track.duration()),
        theme.accent());
  }

  private String errorSVG() {
    return """
        <svg width="400" height="60" xmlns="http://www.w3.org/2000/svg">
          <rect width="400" height="60" fill="#141414" rx="12"/>
          <text x="200" y="36"
                fill="#666" font-family="Arial,sans-serif" font-size="13" text-anchor="middle">
            Nothing playing right now
          </text>
        </svg>
        """;
  }

  private String formatTime(long ms) {
    long sec = ms / 1000;
    return String.format("%d:%02d", sec / 60, sec % 60);
  }

  private String truncateText(String text, int max) {
    if (text == null)
      return "";
    String escaped = escapeXml(text);
    return escaped.length() <= max ? escaped : escaped.substring(0, max - 1) + "…";
  }

  private String escapeXml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private String getAlbumImageAsBase64(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank())
      return "";

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(imageUrl))
            .header("User-Agent", "Mozilla/5.0 (compatible; music-widget/1.0)")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build();

        HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
          System.err.println("Image fetch failed: " + res.statusCode() + " attempt " + (attempt + 1));
          continue;
        }

        String mime = res.headers()
            .firstValue("content-type")
            .orElse("image/jpeg")
            .split(";")[0]
            .trim();

        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(res.body());

      } catch (Exception e) {
        System.err.println("Image fetch error attempt " + (attempt + 1) + ": " + e.getMessage());
      }
    }
    return "";
  }
}
