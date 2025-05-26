package io.quarkiverse.mcp.google.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;

@Path("/google")
public class GoogleOAuthResource {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleOAuthConfig config;
    private final CredentialManager credentialManager;

    public GoogleOAuthResource(GoogleOAuthConfig config, CredentialManager credentialManager) {
        this.config = config;
        this.credentialManager = credentialManager;
    }

    @GET
    @Path("/auth")
    @Produces(MediaType.TEXT_PLAIN)
    public Response auth() throws Exception {
        AuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                config.clientId(),
                config.clientSecret(),
                Collections.singleton(config.scope()))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(config.redirectUri()).build();
        return Response.status(Response.Status.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GET
    @Path("/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    public Response credentials() {
        try {
            Credential credential = credentialManager.getStoredCredential();
            if (credential != null && credential.getAccessToken() != null) {
                return Response.ok(credential).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("No valid credentials found. Please authenticate first.").build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("Failed to retrieve credentials: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/oauth2callback")
    public Response callback(@QueryParam("code") String code) {
        try {
            Credential credential = credentialManager.saveCredential(code);
            return Response.ok("Credential saved to disk").build();
        } catch (Exception e) {
            return Response.serverError().entity("Failed to save credential: " + e.getMessage()).build();
        }
    }
}
