package io.quarkiverse.mcp.auth.facebook;

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
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(FacebookOAuthResourceTest.class)
public class FacebookOAuthResourceTest implements QuarkusTestProfile {

    @InjectMock
    @MockitoConfig(convertScopes = true)
    FacebookCredentialStore credentialStore;

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "facebook.client.id", "some-client-id",
                "facebook.client.secret", "some-client-secret",
                "facebook.redirect.uri", "http://localhost:8080/facebook/oauth2callback",
                "facebook.scope", "email,public_profile"
        );
    }

    @Test
    void testAuthEndpoint() throws Exception {
        var response = given()
                .redirects()
                .follow(false)
          .when().get("/facebook/auth");
        assertThat(response.statusCode(), is(Response.Status.FOUND.getStatusCode()));
        assertThat(response.body().print(), is(emptyOrNullString()));
        URI uri = new URI(response.getHeader("Location"));
        assertThat(uri.getScheme(), is("https"));
        assertThat(uri.getHost(), is("www.facebook.com"));
        assertThat(uri.getPath(), is("/dialog/oauth"));
        var params = Arrays.stream(uri.getQuery().split("&"))
                .map(param -> param.split("="))
                .collect(toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        assertThat(params.get("client_id"), is("some-client-id"));
        assertThat(params.get("redirect_uri"), is("http://localhost:8080/facebook/oauth2callback"));
        assertThat(params.get("response_type"), is("code"));
        assertThat(params.get("scope"), is("email,public_profile"));
        assertThat(params.get("state"), is(notNullValue()));
    }

    @Test
    void testCallbackEndpoint() throws Exception {
        String code = "some-code";
        String state = "some-state";
        var response = given()
                .queryParam("code", code)
                .queryParam("state", state)
          .when().get("/facebook/oauth2callback");
        Mockito.verify(credentialStore).saveCredential(code);
        assertThat(response.statusCode(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.body().asString(), is("Credential saved to disk"));
    }
}