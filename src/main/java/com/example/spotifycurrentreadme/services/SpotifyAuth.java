package com.example.spotifycurrentreadme.services;

import com.example.spotifycurrentreadme.types.AuthPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.NotFoundException;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Service
public class SpotifyAuth {
    @Value("${spotify.sp_dc}")
    private String SP_DC;
    private CodeGenerator codeGenerator = new DefaultCodeGenerator(
            HashingAlgorithm.SHA1,
            6
    );
    private TimeProvider timeProvider = new SystemTimeProvider();
    private final int FETCH_INTERVAL = 60 * 60 * 1000;
    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";
    private final String SECRETS_URL = "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretDict.json";
    private String currentTotpVersion = null;
    private String currentTotp = null;
    private String updatePromise = null;
    private Date lastFetchTime = new Date(0);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SpotifyAuth() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private String createTotpSecret(int[] data) {
        // XOR transformation
        int[] mappedData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            mappedData[i] = data[i] ^ ((i % 33) + 9);
        }
        // convert to string and get hex
        StringBuilder sb = new StringBuilder();
        for (int value : mappedData) {
            sb.append(value);
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String hexData = Hex.encodeHexString(bytes);

        return secretFromHex(hexData);
    }

    private String secretFromHex(String hexData) {
        try {
            byte[] bytes = Hex.decodeHex(hexData);
            Base32 base32 = new Base32();
            return base32.encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert hex to Base32", e);
        }
    }

    private Map<String, Object> fetchSecretsFromGitHub() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SECRETS_URL))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", userAgent)
                .build();

        HttpResponse<String> response = httpClient
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception(
                    String.format("HTTP %d: %s",
                            response.statusCode(),
                            response.statusCode()
                    )
            );
        }

        return objectMapper.readValue(
                response.body(),
                Map.class
        );

    }

    private String findNewestVersion(Map<String, Object> secrets) {
        int[] versions = secrets.keySet().stream()
                .mapToInt(Integer::parseInt)
                .toArray();

        return String.valueOf(Arrays.stream(versions).max().orElseThrow());
    }

    private void useFallbackSecret() throws CodeGenerationException {
        // most likely will fail cus spotify rotates secrets often
        // v61
        int[] fallbackData = {
                44, 55, 47, 42, 70, 40, 34, 114, 76, 74, 50, 111, 120, 97, 75, 76, 94, 102, 43, 69, 49, 120, 118, 80, 64, 78
        };
        currentTotpVersion = "19";
        currentTotp = createTotpSecret(fallbackData);

    }

    private void updateTotpSecrets() {
        if(updatePromise != null) {
            return;
        }
        try {
            Date now = new Date();
            if (FETCH_INTERVAL > (now.getTime() - lastFetchTime.getTime())) {
                return;
            }
            Map<String, Object> secrets = fetchSecretsFromGitHub();
            String newestVersion = findNewestVersion(secrets);

            if (!newestVersion.equals(currentTotpVersion)){
                @SuppressWarnings("unchecked")
                ArrayList<Integer> secretList = (ArrayList<Integer>) secrets.get(newestVersion);
                int[] secretData = secretList.stream().mapToInt(Integer::intValue).toArray();
                
                currentTotp = createTotpSecret(secretData);
                currentTotpVersion = newestVersion;
                lastFetchTime = now;
            } else {
                lastFetchTime = now;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            updatePromise = null;
        }
    }

    private void initializeTotpSecrets() throws CodeGenerationException {
        try {
            updateTotpSecrets();
        } catch (Exception ex) {
            System.err.println("Failed to initialize TotpSecrets, falling back" + ex.getMessage());
            useFallbackSecret();
        }
    }

    private String generateTOTP(long timestamp) {
        try {
            if(currentTotp == null) {
                throw new Exception("TOTP not initialized");
            }
            return codeGenerator.generate(currentTotp, timestamp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP not initialized", e);
        }
    }

    private Integer getServerTime() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://open.spotify.com/api/server-time"))
                    .GET()
                    .header("User-Agent", userAgent)
                    .header("Origin", "https://open.spotify.com")
                    .header("Referer", "https://open.spotify.com/")
                    .header("Cookie", "sp_dc=" + SP_DC)
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get server time: HTTP " + response.statusCode());
            }
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            return (Integer) responseBody.get("serverTime");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthPayload generateAuthPayload(String reason, String productType) {
        long localTime = System.currentTimeMillis();
        Integer serverTime = getServerTime();

        String totp = generateTOTP(localTime / 1000 / 30);
        String totpVer = currentTotpVersion != null ? currentTotpVersion : "19";
        String totpServer = generateTOTP(serverTime / 30);

        return new AuthPayload(reason, productType, totp, totpVer, totpServer);
    }

    public String getToken() {
        String reason = "init";
        String productType = "mobile-web-player";

        try {
            if (currentTotp == null) {
                initializeTotpSecrets();
            } else {
                if (System.currentTimeMillis() - lastFetchTime.getTime() >= FETCH_INTERVAL) {
                    try {
                        updateTotpSecrets();
                    } catch (Exception e) {
                        System.err.println("Failed to update TOTP secrets, continuing with current version: " + e.getMessage());
                    }
                }
            }

            AuthPayload payload = generateAuthPayload(reason, productType);

            Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
            StringBuilder urlBuilder = new StringBuilder("https://open.spotify.com/api/token");
            if (!payloadMap.isEmpty()) {
                urlBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                    if (!first) {
                        urlBuilder.append("&");
                    }
                    first = false;
                    urlBuilder.append(java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", userAgent)
                    .header("Origin", "https://open.spotify.com/")
                    .header("Referer", "https://open.spotify.com/")
                    .header("Cookie", "sp_dc=" + SP_DC)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }

            Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
            return (String) data.get("accessToken");
        } catch (CodeGenerationException e) {
            throw new RuntimeException(e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
