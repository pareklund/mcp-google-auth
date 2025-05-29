package io.quarkiverse.mcp.auth.google;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.MockitoConfig;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;

@QuarkusTest
@TestProfile(GoogleOAUthResourceTest.class)
public class GoogleOAUthResourceTest implements QuarkusTestProfile {

    @InjectMock
    @MockitoConfig(convertScopes = true)
    GoogleCredentialStore credentialStore;

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "google.client.id", "some-client-id",
                "google.client.secret", "some-client-secret",
                "google.redirect.uri", "http://localhost:8080/google/oauth2callback",
                "google.scope", "https://www.googleapis.com/auth/drive"
        );
    }

    @Test
    void testAuthEndpoint() throws Exception {
        var response = given()
                .redirects()
                .follow(false)
          .when().get("/google/auth");
        assertThat(response.statusCode(), is(Response.Status.FOUND.getStatusCode()));
        assertThat(response.body().print(), is(emptyOrNullString()));
        URI uri = new URI(response.getHeader("Location"));
        assertThat(uri.getScheme(), is("https"));
        assertThat(uri.getHost(), is("accounts.google.com"));
        assertThat(uri.getPath(), is("/o/oauth2/auth"));
        var params = Arrays.stream(uri.getQuery().split("&"))
                .map(param -> param.split("="))
                .collect(toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        assertThat(params.get("client_id"), is("some-client-id"));
        assertThat(params.get("redirect_uri"), is("http://localhost:8080/google/oauth2callback"));
        assertThat(params.get("response_type"), is("code"));
        assertThat(params.get("scope"), is("https://www.googleapis.com/auth/drive"));
        assertThat(params.get("access_type"), is("offline"));
        assertThat(params.get("approval_prompt"), is("force"));
    }

    @Test
    void testCallbackEndpoint() throws Exception {
        String code = "some-code"; // Simulate a code received from Google OAuth
        var response = given()
                .queryParam("code", code)
          .when().get("/google/oauth2callback");
        Mockito.verify(credentialStore).saveCredential(code);
        assertThat(response.statusCode(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.body().asString(), is("Credential saved to disk"));
    }
}