package io.quarkiverse.mcp.google.auth;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "google")
public interface GoogleOAuthConfig {
    @WithName("client.id") String clientId();
    @WithName("client.secret") String clientSecret();
    @WithName("redirect.uri") String redirectUri();
    @WithName("scope") String scope();
}
