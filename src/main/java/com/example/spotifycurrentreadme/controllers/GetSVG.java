package com.example.spotifycurrentreadme.controllers;

import com.example.spotifycurrentreadme.services.SpotifyAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/get-svg")
public class GetSVG {
    private final SpotifyAuth spotifyAuth;

    public GetSVG(SpotifyAuth spotifyAuth) {
        this.spotifyAuth = spotifyAuth;
    }
//    test
//    @GetMapping("/")
//    public String getToken() {
//        return spotifyAuth.getToken();
//    }

}