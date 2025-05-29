package io.quarkiverse.mcp.auth.facebook;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = FacebookOAuthConfig.PREFIX)
public interface FacebookOAuthConfig {
    String PREFIX = "facebook";
    String CLIENT_ID = "client.id";
    String CLIENT_SECRET = "client.secret";
    String REDIRECT_URI = "redirect.uri";
    String SCOPE = "scope";

    @WithName(CLIENT_ID) String clientId();
    @WithName(CLIENT_SECRET) String clientSecret();
    @WithName(REDIRECT_URI) String redirectUri();
    @WithName(SCOPE) String scope();
}