package com.example.spotifycurrentreadme.controllers;

import com.example.spotifycurrentreadme.services.TrackService;
import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/get-svg")
public class GetSVG {
    private final TrackService trackService;

    public GetSVG(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/")
    public CurrentPlayingRes getCurrentTrack() {
        return trackService.getTrackInfo();
    }
}