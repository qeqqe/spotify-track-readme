package com.example.spotifycurrentreadme.interceptor;

import com.example.spotifycurrentreadme.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String clientIp = getClientIp(request);
        Bucket bucket = rateLimitConfig.resolveBucket(clientIp);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(429);
            response.setContentType("image/svg+xml;charset=UTF-8");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(generateRateLimitSVG(waitForRefill));
            return false;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateRateLimitSVG(long retryAfter) {
        return """
            <svg width="400" height="120" xmlns="http://www.w3.org/2000/svg">
                <rect width="400" height="120" fill="#f44336" rx="10"/>
                <text x="200" y="55" fill="white" 
                      font-family="Arial" font-size="16" 
                      font-weight="bold"
                      text-anchor="middle">
                    ⚠️ Rate Limit Exceeded
                </text>
                <text x="200" y="80" fill="white" 
                      font-family="Arial" font-size="12" 
                      text-anchor="middle">
                    Try again in %d seconds
                </text>
            </svg>
            """.formatted(retryAfter);
    }
}
