package com.example.spotifycurrentreadme.services;

import com.example.spotifycurrentreadme.types.CurrentPlayingRes;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
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
        int remainingSeconds = calculateRemainingSeconds(track.durationMs(), track.progressMs());

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
                    <clipPath id="album-clip">
                        <rect x="10" y="10" width="100" height="100" rx="8"/>
                    </clipPath>
                </defs>
                
                <style>
                    @keyframes progressBar {
                        from { width: %dpx; }
                        to { width: 270px; }
                    }
                    
                    @keyframes pulse {
                        0%%, 100%% { opacity: 1; }
                        50%% { opacity: 0.3; }
                    }
                    
                    @keyframes eq1 {
                        0%%, 100%% { height: 8px; }
                        25%% { height: 15px; }
                        50%% { height: 12px; }
                        75%% { height: 18px; }
                    }
                    
                    @keyframes eq1Y {
                        0%%, 100%% { y: 92px; }
                        25%% { y: 85px; }
                        50%% { y: 88px; }
                        75%% { y: 82px; }
                    }
                    
                    @keyframes eq2 {
                        0%%, 100%% { height: 15px; }
                        25%% { height: 12px; }
                        50%% { height: 18px; }
                        75%% { height: 10px; }
                    }
                    
                    @keyframes eq2Y {
                        0%%, 100%% { y: 85px; }
                        25%% { y: 88px; }
                        50%% { y: 82px; }
                        75%% { y: 90px; }
                    }
                    
                    @keyframes eq3 {
                        0%%, 100%% { height: 12px; }
                        25%% { height: 18px; }
                        50%% { height: 10px; }
                        75%% { height: 8px; }
                    }
                    
                    @keyframes eq3Y {
                        0%%, 100%% { y: 88px; }
                        25%% { y: 82px; }
                        50%% { y: 90px; }
                        75%% { y: 92px; }
                    }
                    
                    @keyframes eq4 {
                        0%%, 100%% { height: 18px; }
                        25%% { height: 10px; }
                        50%% { height: 8px; }
                        75%% { height: 15px; }
                    }
                    
                    @keyframes eq4Y {
                        0%%, 100%% { y: 82px; }
                        25%% { y: 90px; }
                        50%% { y: 92px; }
                        75%% { y: 85px; }
                    }
                    
                    @keyframes eq5 {
                        0%%, 100%% { height: 10px; }
                        25%% { height: 8px; }
                        50%% { height: 15px; }
                        75%% { height: 12px; }
                    }
                    
                    @keyframes eq5Y {
                        0%%, 100%% { y: 90px; }
                        25%% { y: 92px; }
                        50%% { y: 85px; }
                        75%% { y: 88px; }
                    }
                    
                    .progress-bar {
                        animation: progressBar %ds linear forwards;
                    }
                    
                    .pulse-indicator {
                        animation: pulse 1.5s ease-in-out infinite;
                    }
                    
                    .eq-bar-1 {
                        animation: eq1 0.6s ease-in-out infinite, eq1Y 0.6s ease-in-out infinite;
                    }
                    
                    .eq-bar-2 {
                        animation: eq2 0.7s ease-in-out infinite, eq2Y 0.7s ease-in-out infinite;
                    }
                    
                    .eq-bar-3 {
                        animation: eq3 0.8s ease-in-out infinite, eq3Y 0.8s ease-in-out infinite;
                    }
                    
                    .eq-bar-4 {
                        animation: eq4 0.9s ease-in-out infinite, eq4Y 0.9s ease-in-out infinite;
                    }
                    
                    .eq-bar-5 {
                        animation: eq5 1.0s ease-in-out infinite, eq5Y 1.0s ease-in-out infinite;
                    }
                </style>
                
                <!-- Background -->
                <rect width="400" height="120" fill="url(#grad)" clip-path="url(#rounded)"/>
                
                <!-- Album Art with rounded corners -->
                <image x="10" y="10" width="100" height="100" 
                       href="%s" clip-path="url(#album-clip)"/>
                
                <!-- Song Title -->
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
                
                <!-- Progress Bar Fill with CSS animation -->
                <rect class="progress-bar" x="120" y="70" width="%d" height="4" 
                      fill="#1DB954" rx="2"/>
                
                <!-- Time stamp -->
                <text x="385" y="90" fill="#b3b3b3" 
                      font-family="Arial, sans-serif" 
                      font-size="11" 
                      text-anchor="end">
                    %s
                </text>
                
                <!-- Equalizer bars with CSS animations -->
                <rect class="eq-bar-1" x="130" y="92" width="4" height="8" fill="#1DB954" rx="2"/>
                <rect class="eq-bar-2" x="136" y="85" width="4" height="15" fill="#1DB954" rx="2"/>
                <rect class="eq-bar-3" x="142" y="88" width="4" height="12" fill="#1DB954" rx="2"/>
                <rect class="eq-bar-4" x="148" y="82" width="4" height="18" fill="#1DB954" rx="2"/>
                <rect class="eq-bar-5" x="154" y="90" width="4" height="10" fill="#1DB954" rx="2"/>
                
                <!-- Playing indicator with CSS animation -->
                <circle class="pulse-indicator" cx="385" cy="25" r="3" fill="#1DB954"/>
            </svg>
            """.formatted(
                progressWidth,
                remainingSeconds,
                getAlbumImageAsBase64(track.imageUrl()),
                truncateText(track.name(), 22),
                truncateText(track.artist(), 25),
                progressWidth,
                formatTime(track.durationMs())
        );
    }

    private String generateRecentlyPlayedSVG(CurrentPlayingRes track) {
        long randomProgress = track.durationMs() * (30 + new Random().nextInt(41)) / 100;
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
                getAlbumImageAsBase64(track.imageUrl()),
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

    private String getAlbumImageAsBase64(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            String mimeType = connection.getContentType();
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            java.io.InputStream in = connection.getInputStream();
            byte[] bytes = in.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
