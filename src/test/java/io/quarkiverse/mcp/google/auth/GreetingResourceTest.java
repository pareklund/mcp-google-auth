package io.quarkiverse.mcp.google.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {
    @Test
    @Disabled("FIXME")
    void testHelloEndpoint() {
        given()
          .when().get("/google/auth")
          .then()
             .statusCode(Response.Status.FOUND.getStatusCode())
             .body(is(null));
    }

}