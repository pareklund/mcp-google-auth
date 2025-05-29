package io.quarkiverse.mcp.auth.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class FacebookCredentialStore {

    private static final java.io.File CREDENTIALS_FOLDER = new java.io.File(System.getProperty("user.home"), ".credentials/my-app");

    private final FacebookOAuthConfig config;

    public FacebookCredentialStore(FacebookOAuthConfig config) {
        this.config = config;
    }

    public String saveCredential(String code) throws Exception {
        String accessToken = exchangeCodeForToken(code);
        saveTokenToFile(accessToken);
        saveOAuthConfig();
        return accessToken;
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String tokenUrl = "https://graph.facebook.com/v18.0/oauth/access_token";
        
        Map<String, String> params = new HashMap<>();
        params.put("client_id", config.clientId());
        params.put("client_secret", config.clientSecret());
        params.put("redirect_uri", config.redirectUri());
        params.put("code", code);

        String formData = params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + 
                             URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to exchange code for token: " + response.body());
        }

        String responseBody = response.body();
        var accessTokenResponse = parseAccessTokenResponse(response.body());
        if (accessTokenResponse.access_token() != null) {
            return accessTokenResponse.access_token();
        }
        throw new RuntimeException("No access token found in response: " + responseBody);
    }
    
    record AccessTokenResponse(String access_token, String token_type, int expires_in) {}

    private AccessTokenResponse parseAccessTokenResponse(String responseBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(responseBody, AccessTokenResponse.class);
    }

    private void saveTokenToFile(String accessToken) throws IOException {
        CREDENTIALS_FOLDER.mkdirs();
        try (var writer = new java.io.FileWriter(new java.io.File(CREDENTIALS_FOLDER, "facebook_token"))) {
            writer.write(accessToken);
        }
    }

    private void saveOAuthConfig() {
        CREDENTIALS_FOLDER.mkdirs();
        var properties = new java.util.Properties();
        properties.setProperty(fullPropertyName(FacebookOAuthConfig.CLIENT_ID), config.clientId());
        properties.setProperty(fullPropertyName(FacebookOAuthConfig.CLIENT_SECRET), config.clientSecret());
        properties.setProperty(fullPropertyName(FacebookOAuthConfig.REDIRECT_URI), config.redirectUri());
        properties.setProperty(fullPropertyName(FacebookOAuthConfig.SCOPE), config.scope());
        try (var outputStream = new java.io.FileOutputStream(CREDENTIALS_FOLDER + "/facebook_config.properties")) {
            properties.store(outputStream, "Facebook OAuth Configuration");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save properties", e);
        }
    }

    private static String fullPropertyName(String property) {
        return FacebookOAuthConfig.PREFIX + "." + property;
    }
}