package com.example.spotifycurrentreadme.controllers;

import com.example.spotifycurrentreadme.services.SVGService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(path = "/spotify-track.svg", produces = "image/svg+xml")
    public String getCurrentTrack() {
        return svgService.generateSVG();
    }
}