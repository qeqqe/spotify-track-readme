package com.example.currenttrackreadme.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmTrackInfo(
    @JsonProperty("track") TrackDetail track) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TrackDetail(
      String name,
      String mbid,
      String url,
      String duration,
      String listeners,
      String playcount,
      Streamable streamable,
      Artist artist,
      Album album,
      TopTags toptags,
      Wiki wiki) {
    public long durationMs() {
      return duration != null && !duration.isEmpty() ? Long.parseLong(duration) : 0L;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Streamable(
      @JsonProperty("#text") String text,
      String fulltrack) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Artist(
      String name,
      String mbid,
      String url) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Album(
      String artist,
      String title,
      String url,
      List<Image> image) {
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
  public record Image(
      @JsonProperty("#text") String url,
      String size) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TopTags(List<Tag> tag) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Tag(String name, String url) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Wiki(
      String published,
      String summary,
      String content) {
  }
}
