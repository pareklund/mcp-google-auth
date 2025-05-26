package io.quarkiverse.mcp.google.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Singleton
public class CredentialManager {

    private static final java.io.File CREDENTIALS_FOLDER = new java.io.File(System.getProperty("user.home"), ".credentials/my-app");

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleOAuthConfig config;

    public CredentialManager(GoogleOAuthConfig config) {
        this.config = config;
    }

    Credential getStoredCredential() throws IOException, GeneralSecurityException {
        var flow = createFlow();
        // "user" is a default ID used for single-user apps
        return flow.loadCredential("user");
    }

    Credential saveCredential(String code) throws Exception {
        var flow = createFlow();
        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(config.redirectUri())
                .execute();
        return flow.createAndStoreCredential(response, "user");
    }

    private GoogleAuthorizationCodeFlow createFlow() throws GeneralSecurityException, IOException {
        var dataStoreFactory = new FileDataStoreFactory(CREDENTIALS_FOLDER);

        var clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(config.clientId())
                        .setClientSecret(config.clientSecret()));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList(config.scope()))
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();
    }
}
