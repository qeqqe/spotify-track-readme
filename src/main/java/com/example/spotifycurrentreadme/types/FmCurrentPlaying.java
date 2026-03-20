
package com.example.spotifycurrentreadme.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmCurrentPlaying(
    @JsonProperty("recenttracks") RecentTracks recentTracks) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RecentTracks(
      List<Track> track,
      @JsonProperty("@attr") PageAttr attr) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Track(
      String name,
      String mbid,
      String url,
      Artist artist,
      Album album,
      List<Image> image,
      @JsonProperty("@attr") NowPlayingAttr attr, // null if not currently playing
      TrackDate date // null if currently playing
  ) {
    public boolean isNowPlaying() {
      return attr != null && "true".equals(attr.nowplaying());
    }

    public String coverUrl() {
      if (image == null || image.isEmpty())
        return null;
      return image.stream()
          .filter(i -> "large".equals(i.size()))
          .map(Image::url)
          .findFirst()
          .orElse(image.getLast().url());
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Artist(
      String mbid,
      @JsonProperty("#text") String name) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Album(
      String mbid,
      @JsonProperty("#text") String name) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Image(
      String size,
      @JsonProperty("#text") String url) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record NowPlayingAttr(
      String nowplaying) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TrackDate(
      String uts, // unix timestamp as String
      @JsonProperty("#text") String text) {
    public long utsLong() {
      return Long.parseLong(uts);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PageAttr(
      String user,
      String total,
      String totalPages,
      String page,
      String perPage) {
  }
}
