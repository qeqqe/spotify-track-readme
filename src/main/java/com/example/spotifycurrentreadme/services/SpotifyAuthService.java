package com.example.spotifycurrentreadme.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyAuthService {
    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;
    
    @Value("${spotify.refresh-token}")
    private String refreshToken;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private volatile String cachedAccessToken = null;
    private volatile long expiresAt = 0; // epoch milliseconds
    private final Object lock = new Object();

    public SpotifyAuthService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get a valid OAuth access token. Uses cached token if still valid,
     * otherwise refreshes it using the refresh token.
     * Thread-safe with double-checked locking.
     */
    public String getAccessToken() throws Exception {
        long now = Instant.now().toEpochMilli();
        
        // Fast path: return cached token if still valid
        if (cachedAccessToken != null && now < expiresAt) {
            return cachedAccessToken;
        }

        // Slow path: refresh token (synchronized)
        synchronized (lock) {
            // Double-check: another thread might have refreshed while we waited
            now = Instant.now().toEpochMilli();
            if (cachedAccessToken != null && now < expiresAt) {
                return cachedAccessToken;
            }

            return refreshAccessToken();
        }
    }

    /**
     * Force refresh the access token. Must be called within synchronized block.
     */
    private String refreshAccessToken() throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
        
        String body = "grant_type=refresh_token&refresh_token=" + 
                URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + auth)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Token refresh failed: " + response.statusCode() + " - " + response.body()
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
        
        cachedAccessToken = (String) data.get("access_token");
        int expiresIn = ((Number) data.get("expires_in")).intValue();
        
        // Set expiry with 60 second buffer to avoid edge cases
        expiresAt = Instant.now().toEpochMilli() + (expiresIn * 1000L) - 60_000L;
        
        System.out.println("OAuth token refreshed, expires in " + expiresIn + " seconds");
        
        return cachedAccessToken;
    }

    /**
     * Clear the cached token. Useful for testing or forcing a refresh.
     */
    public void clearCache() {
        synchronized (lock) {
            cachedAccessToken = null;
            expiresAt = 0;
        }
    }
}
