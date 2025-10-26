package com.example.spotifycurrentreadme.services;

import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class SVGService {
    private final TrackService trackService;

    public SVGService(TrackService trackService) {
        this.trackService = trackService;
    }

    public String generateSVG() {
        CurrentPlayingRes track = trackService.getTrackInfo();
        if(track.isPlaying()){
            return generateNowPlayingSVG(track);
        } else {
            return generateRecentlyPlayedSVG(track);
        }
    }

    private String generateNowPlayingSVG(CurrentPlayingRes track){
        int progressWidth = calculateProgressWidth(track.durationMs(), track.progressMs());
        // svg is not my thing ok?
        return """
            <svg width="400" height="120" xmlns="http://www.w3.org/2000/svg">
                <defs>
                    <linearGradient id="grad" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                        <stop offset="0%%" style="stop-color:#1DB954;stop-opacity:1" />
                        <stop offset="100%%" style="stop-color:#191414;stop-opacity:1" />
                    </linearGradient>
                    <clipPath id="rounded">
                        <rect width="400" height="120" rx="10" ry="10"/>
                    </clipPath>
                </defs>
                
                <!-- Background -->
                <rect width="400" height="120" fill="url(#grad)" clip-path="url(#rounded)"/>
                
                <!-- Album Art with rounded corners -->
                <clipPath id="album-clip">
                    <rect x="10" y="10" width="100" height="100" rx="8"/>
                </clipPath>
                <image x="10" y="10" width="100" height="100" 
                       href="%s" clip-path="url(#album-clip)"/>
                
                <!-- Song Title (with truncation) -->
                <text x="120" y="35" fill="white" 
                      font-family="Arial, sans-serif" 
                      font-size="16" 
                      font-weight="bold">
                    %s
                </text>
                
                <!-- Artist Name -->
                <text x="120" y="55" fill="#b3b3b3" 
                      font-family="Arial, sans-serif" 
                      font-size="13">
                    %s
                </text>
                
                <!-- Progress Bar Background -->
                <rect x="120" y="70" width="270" height="4" 
                      fill="#404040" rx="2"/>
                
                <!-- Progress Bar Fill with animation -->
                <rect x="120" y="70" width="%d" height="4" 
                      fill="#1DB954" rx="2">
                    <animate attributeName="width" 
                             from="%d" 
                             to="270" 
                             dur="%ds" 
                             fill="freeze"/>
                </rect>
                
                <!-- Time stamps -->
                <text x="385" y="90" fill="#b3b3b3" 
                      font-family="Arial, sans-serif" 
                      font-size="11" 
                      text-anchor="end">
                    %s
                </text>
                
                <!-- Equalizer bars -->
                %s
                
                <!-- Playing indicator -->
                <circle cx="385" cy="25" r="3" fill="#1DB954">
                    <animate attributeName="opacity" 
                             values="1;0.3;1" 
                             dur="1.5s" 
                             repeatCount="indefinite"/>
                </circle>
            </svg>
            """.formatted(
                    track.imageUrl(),
                    truncateText(track.name(),22),
                    truncateText(track.artist(), 25),
                    progressWidth,
                    progressWidth,
                    calculateRemainingSeconds(track.durationMs(), track.progressMs()),
                    formatTime(track.durationMs()),
                    generateEqualizerBars()
        );
    }


    private String generateRecentlyPlayedSVG(CurrentPlayingRes track) {
        long randomProgress = track.durationMs() * (30 + new Random().nextInt(41)) / 100;
        // since we get no progress_ms in recent tracks, we just go random...
        int progressWidth = calculateProgressWidth(track.durationMs(), randomProgress);

        return """
        <svg width="400" height="120" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="recent-grad" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                    <stop offset="0%%" style="stop-color:#2a2a2a;stop-opacity:1" />
                    <stop offset="100%%" style="stop-color:#1a1a1a;stop-opacity:1" />
                </linearGradient>
                <clipPath id="rounded">
                    <rect width="400" height="120" rx="10" ry="10"/>
                </clipPath>
                <clipPath id="album-clip">
                    <rect x="10" y="10" width="100" height="100" rx="8"/>
                </clipPath>
            </defs>
            
            <!-- Background (muted dark) -->
            <rect width="400" height="120" fill="url(#recent-grad)" clip-path="url(#rounded)"/>
            
            <!-- Album Art (slightly dimmed) -->
            <image x="10" y="10" width="100" height="100" 
                   href="%s" 
                   clip-path="url(#album-clip)"
                   opacity="0.7"/>
            
            <!-- Song Title -->
            <text x="120" y="30" fill="#b3b3b3" 
                  font-family="Arial, sans-serif" 
                  font-size="15" 
                  font-weight="600">
                %s
            </text>
            
            <!-- Artist Name -->
            <text x="120" y="50" fill="#888888" 
                  font-family="Arial, sans-serif" 
                  font-size="12">
                %s
            </text>
            
            <!-- "Recently played" indicator -->
            <text x="120" y="68" fill="#666666" 
                  font-family="Arial, sans-serif" 
                  font-size="10"
                  font-style="italic">
                Recently played
            </text>
            
            <!-- Progress Bar Background (dimmed) -->
            <rect x="120" y="78" width="270" height="3" 
                  fill="#2a2a2a" rx="2"/>
            
            <!-- Static Progress Bar (no animation) -->
            <rect x="120" y="78" width="%d" height="3" 
                  fill="#666666" rx="2"/>
            
            <!-- Time stamps (muted) -->
            <text x="120" y="95" fill="#666666" 
                  font-family="Arial, sans-serif" 
                  font-size="10">
                %s
            </text>
            <text x="385" y="95" fill="#666666" 
                  font-family="Arial, sans-serif" 
                  font-size="10" 
                  text-anchor="end">
                %s
            </text>
            
            <!-- Paused indicator (replaces playing dot) -->
            <circle cx="385" cy="25" r="3" fill="#666666" opacity="0.6"/>
        </svg>
        """.formatted(
                track.imageUrl(),
                truncateText(track.name(), 22),
                truncateText(track.artist(), 30),
                progressWidth,
                formatTime(randomProgress),
                formatTime(track.durationMs())
        );
    }
    private int calculateProgressWidth(long durationMs, long progressMs) {
        double progress = (double) progressMs / durationMs;
        return (int) (270 * progress);
    }

    private int calculateRemainingSeconds(long durationMs, long progressMs) {
        return (int) ((durationMs - progressMs) / 1000);
    }

    private String generateEqualizerBars() {
        StringBuilder bars = new StringBuilder();
        int[] heights = {8, 15, 12, 18, 10};

        for (int i = 0; i < 5; i++) {
            int x = 130 + (i * 6);
            bars.append("""
                <rect x="%d" y="%d" width="4" height="%d" fill="#1DB954" rx="2">
                    <animate attributeName="height" 
                             values="%d;%d;%d;%d;%d" 
                             dur="%ss" 
                             repeatCount="indefinite"/>
                    <animate attributeName="y" 
                             values="%d;%d;%d;%d;%d" 
                             dur="%ss" 
                             repeatCount="indefinite"/>
                </rect>
                """.formatted(
                    x, 100 - heights[i], heights[i],
                    heights[i], heights[(i+1)%5], heights[(i+2)%5], heights[(i+3)%5], heights[i],
                    0.6 + (i * 0.1),
                    100 - heights[i], 100 - heights[(i+1)%5], 100 - heights[(i+2)%5], 100 - heights[(i+3)%5], 100 - heights[i],
                    0.6 + (i * 0.1)
            ));
        }

        return bars.toString();
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return escapeXml(text);
        }
        return escapeXml(text.substring(0, maxLength - 3)) + "...";
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
