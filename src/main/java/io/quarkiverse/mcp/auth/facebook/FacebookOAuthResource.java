package io.quarkiverse.mcp.auth.facebook;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Path("/facebook")
public class FacebookOAuthResource {

    private final FacebookOAuthConfig config;
    private final FacebookCredentialStore credentialStore;

    public FacebookOAuthResource(FacebookOAuthConfig config, FacebookCredentialStore credentialStore) {
        this.config = config;
        this.credentialStore = credentialStore;
    }

    @GET
    @Path("/auth")
    @Produces(MediaType.TEXT_PLAIN)
    public Response auth() throws Exception {
        String state = UUID.randomUUID().toString();
        
        String authorizationUrl = "https://www.facebook.com/dialog/oauth" +
                "?client_id=" + URLEncoder.encode(config.clientId(), StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(config.redirectUri(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(config.scope(), StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                "&response_type=code";

        return Response.status(Response.Status.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GET
    @Path("/oauth2callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        try {
            credentialStore.saveCredential(code);
            return Response.ok("Credential saved to disk").build();
        } catch (Exception e) {
            return Response.serverError().entity("Failed to save credential: " + e.getMessage()).build();
        }
    }
}