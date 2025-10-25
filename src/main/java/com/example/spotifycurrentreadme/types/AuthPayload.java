package com.example.spotifycurrentreadme.types;

public class AuthPayload {
    public String reason;
    public String productType;
    public String totp;
    public String totpVer;
    public String totpServer;

    public AuthPayload(String reason, String productType, String totp, String totpVer, String totpServer) {
        this.reason = reason;
        this.productType = productType;
        this.totp = totp;
        this.totpVer = totpVer;
        this.totpServer = totpServer;
    }
}