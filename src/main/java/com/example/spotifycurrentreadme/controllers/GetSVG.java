package com.example.spotifycurrentreadme.controllers;

import com.example.spotifycurrentreadme.services.SVGService;
import com.example.spotifycurrentreadme.services.TrackService;
import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import com.example.spotifycurrentreadme.types.SpotifyCurrentlyPlaying;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/get-svg")
public class GetSVG {
    private final SVGService svgService;
    private final TrackService trackService;

    public GetSVG(SVGService svgService, TrackService trackService) {
        this.svgService = svgService;
        this.trackService = trackService;
    }

    @GetMapping(path = "/", produces = "image/svg+xml")
    public String getCurrentTrack() {
        return svgService.generateSVG();
    }

    @GetMapping("/test")
    public CurrentPlayingRes test() {
        return trackService.getTrackInfo();
    }
}