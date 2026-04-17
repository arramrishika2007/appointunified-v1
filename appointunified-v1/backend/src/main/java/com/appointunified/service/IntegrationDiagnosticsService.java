package com.appointunified.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntegrationDiagnosticsService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${NEON_DATABASE_URL:}")
    private String databaseUrl;

    @Value("${REDIS_URL:}")
    private String redisUrl;

    @Value("${UPSTASH_REDIS_REST_URL:}")
    private String upstashRestUrl;

    @Value("${UPSTASH_REDIS_REST_TOKEN:}")
    private String upstashRestToken;

    @Value("${RAZORPAY_KEY_ID:}")
    private String razorpayKeyId;

    @Value("${RAZORPAY_KEY_SECRET:}")
    private String razorpayKeySecret;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${FIREBASE_CREDENTIALS:}")
    private String firebaseCredentials;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    @Value("${CLOUDINARY_CLOUD_NAME:}")
    private String cloudinaryCloudName;

    @Value("${CLOUDINARY_API_KEY:}")
    private String cloudinaryApiKey;

    @Value("${CLOUDINARY_API_SECRET:}")
    private String cloudinaryApiSecret;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${TWILIO_ACCOUNT_SID:}")
    private String twilioAccountSid;

    @Value("${TWILIO_AUTH_TOKEN:}")
    private String twilioAuthToken;

    @Value("${MONGODB_URI:}")
    private String mongodbUri;

    @Value("${SUPABASE_URL:}")
    private String supabaseUrl;

    @Value("${SUPABASE_SERVICE_ROLE_KEY:}")
    private String supabaseServiceRoleKey;

    @Value("${SUPABASE_ANON_KEY:}")
    private String supabaseAnonKey;

    public Map<String, Object> getIntegrationStatusReport() {
        List<Map<String, Object>> checks = new ArrayList<>();

        checks.add(checkJdbcDns("neon_postgres", databaseUrl));
        checks.add(checkRedisDns("redis_tcp", redisUrl));
        checks.add(checkUpstashRest("upstash_rest", upstashRestUrl, upstashRestToken));
        checks.add(checkFirebaseConfigured());
        checks.add(checkRazorpayApi("razorpay", razorpayKeyId, razorpayKeySecret));
        checks.add(checkGroqApi("groq", groqApiKey));
        checks.add(checkCloudinaryConfigured());
        checks.add(checkResend("resend", resendApiKey));
        checks.add(checkTwilio("twilio", twilioAccountSid, twilioAuthToken));
        checks.add(checkMongoDns("mongodb", mongodbUri));
        checks.add(checkSupabase("supabase", supabaseUrl, supabaseServiceRoleKey, supabaseAnonKey));

        int configured = 0;
        int healthy = 0;
        for (Map<String, Object> item : checks) {
            if (Boolean.TRUE.equals(item.get("configured"))) {
                configured++;
            }
            if ("ok".equals(item.get("status")) || "reachable".equals(item.get("status"))) {
                healthy++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", checks.size());
        summary.put("configured", configured);
        summary.put("healthyOrReachable", healthy);
        summary.put("checkedAt", OffsetDateTime.now().toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("integrations", checks);
        return result;
    }

    private Map<String, Object> checkJdbcDns(String name, String jdbcOrUrl) {
        if (isBlank(jdbcOrUrl)) {
            return status(name, false, "not_configured", "NEON_DATABASE_URL is empty");
        }

        String host = extractHostFromJdbc(jdbcOrUrl);
        if (isBlank(host)) {
            return status(name, true, "invalid", "Could not parse DB host");
        }

        return resolveHost(name, host);
    }

    private Map<String, Object> checkRedisDns(String name, String redisUri) {
        if (isBlank(redisUri)) {
            return status(name, false, "not_configured", "REDIS_URL is empty");
        }

        try {
            URI uri = URI.create(redisUri);
            if (isBlank(uri.getHost())) {
                return status(name, true, "invalid", "Could not parse Redis host");
            }
            return resolveHost(name, uri.getHost());
        } catch (Exception ex) {
            return status(name, true, "invalid", "Invalid REDIS_URL format");
        }
    }

    private Map<String, Object> checkUpstashRest(String name, String url, String token) {
        if (isBlank(url) || isBlank(token)) {
            return status(name, false, "not_configured", "UPSTASH_REDIS_REST_URL/TOKEN missing");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 500) {
                return status(name, true, "reachable", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> checkFirebaseConfigured() {
        if (isBlank(firebaseCredentials)) {
            return status("firebase_admin", false, "not_configured", "FIREBASE_CREDENTIALS missing");
        }
        return status("firebase_admin", true, "configured", "Credentials present");
    }

    private Map<String, Object> checkRazorpayApi(String name, String keyId, String keySecret) {
        if (isBlank(keyId) || isBlank(keySecret)) {
            return status(name, false, "not_configured", "RAZORPAY_KEY_ID/SECRET missing");
        }

        try {
            String authToken = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders?count=1"))
                    .timeout(Duration.ofSeconds(8))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return status(name, true, "ok", "Razorpay API reachable");
            }
            if (code == 401 || code == 403) {
                return status(name, true, "invalid_credentials", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> checkGroqApi(String name, String key) {
        if (isBlank(key)) {
            return status(name, false, "not_configured", "GROQ_API_KEY missing");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/models"))
                    .timeout(Duration.ofSeconds(8))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return status(name, true, "ok", "Groq API reachable");
            }
            if (code == 401 || code == 403) {
                return status(name, true, "invalid_credentials", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> checkCloudinaryConfigured() {
        boolean configured = !isBlank(cloudinaryUrl)
                || (!isBlank(cloudinaryCloudName) && !isBlank(cloudinaryApiKey) && !isBlank(cloudinaryApiSecret));

        if (!configured) {
            return status("cloudinary", false, "not_configured", "Cloudinary credentials missing");
        }

        return resolveHost("cloudinary", "api.cloudinary.com");
    }

    private Map<String, Object> checkResend(String name, String apiKey) {
        if (isBlank(apiKey)) {
            return status(name, false, "not_configured", "RESEND_API_KEY missing");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/domains"))
                    .timeout(Duration.ofSeconds(8))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return status(name, true, "ok", "Resend API reachable");
            }
            if (code == 401 || code == 403) {
                return status(name, true, "invalid_credentials", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> checkTwilio(String name, String sid, String token) {
        if (isBlank(sid) || isBlank(token)) {
            return status(name, false, "not_configured", "TWILIO_ACCOUNT_SID/AUTH_TOKEN missing");
        }

        try {
            String authToken = Base64.getEncoder().encodeToString((sid + ":" + token).getBytes());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + ".json"))
                    .timeout(Duration.ofSeconds(8))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return status(name, true, "ok", "Twilio API reachable");
            }
            if (code == 401 || code == 403) {
                return status(name, true, "invalid_credentials", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> checkMongoDns(String name, String uri) {
        if (isBlank(uri)) {
            return status(name, false, "not_configured", "MONGODB_URI missing");
        }

        String host = extractMongoHost(uri);
        if (isBlank(host)) {
            return status(name, true, "invalid", "Could not parse Mongo host");
        }

        return resolveHost(name, host);
    }

    private Map<String, Object> checkSupabase(String name, String url, String serviceKey, String anonKey) {
        String apiKey = !isBlank(serviceKey) ? serviceKey : anonKey;
        if (isBlank(url) || isBlank(apiKey)) {
            return status(name, false, "not_configured", "SUPABASE_URL or key missing");
        }

        try {
            String normalized = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalized + "/rest/v1/"))
                    .timeout(Duration.ofSeconds(8))
                    .header("apikey", apiKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 500) {
                return status(name, true, "reachable", "HTTP " + code);
            }
            return status(name, true, "error", "HTTP " + code);
        } catch (Exception ex) {
            return status(name, true, "error", trimMessage(ex.getMessage()));
        }
    }

    private Map<String, Object> resolveHost(String name, String host) {
        try {
            InetAddress.getByName(host);
            return status(name, true, "ok", "DNS resolved: " + host);
        } catch (Exception ex) {
            return status(name, true, "dns_error", trimMessage(ex.getMessage()));
        }
    }

    private String extractHostFromJdbc(String jdbcUrl) {
        String cleaned = jdbcUrl;
        if (jdbcUrl.startsWith("jdbc:")) {
            cleaned = jdbcUrl.substring(5);
        }
        try {
            URI uri = new URI(cleaned);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private String extractMongoHost(String uri) {
        try {
            String cleaned = uri;
            if (cleaned.startsWith("mongodb://")) {
                cleaned = cleaned.substring("mongodb://".length());
            } else if (cleaned.startsWith("mongodb+srv://")) {
                cleaned = cleaned.substring("mongodb+srv://".length());
            }

            int atIndex = cleaned.lastIndexOf('@');
            if (atIndex >= 0 && atIndex + 1 < cleaned.length()) {
                cleaned = cleaned.substring(atIndex + 1);
            }

            int slashIndex = cleaned.indexOf('/');
            if (slashIndex > -1) {
                cleaned = cleaned.substring(0, slashIndex);
            }

            int commaIndex = cleaned.indexOf(',');
            if (commaIndex > -1) {
                cleaned = cleaned.substring(0, commaIndex);
            }

            int colonIndex = cleaned.indexOf(':');
            if (colonIndex > -1) {
                cleaned = cleaned.substring(0, colonIndex);
            }

            return cleaned;
        } catch (Exception ex) {
            return "";
        }
    }

    private Map<String, Object> status(String name, boolean configured, String status, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("configured", configured);
        map.put("status", status);
        map.put("detail", detail);
        map.put("checkedAt", OffsetDateTime.now().toString());
        return map;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "unknown error";
        }
        return msg.length() > 240 ? msg.substring(0, 240) : msg;
    }
}
