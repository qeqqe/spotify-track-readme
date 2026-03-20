package com.example.spotifycurrentreadme.controllers;

import com.example.spotifycurrentreadme.services.SVGService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/")
public class GetSVG {
  private final SVGService svgService;

  public GetSVG(SVGService svgService) {
    this.svgService = svgService;
  }

  @GetMapping("/health")
  public String healthCheck() {
    return "OK";
  }

  @GetMapping(path = "/current-track.svg", produces = "image/svg+xml; charset=utf-8")
  public ResponseEntity<String> getCurrentTrack() {
    String svg = svgService.generateSVG();

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(120, TimeUnit.SECONDS).cachePublic())
        .header("Content-Type", "image/svg+xml; charset=utf-8")
        .body(svg);
  }
}
