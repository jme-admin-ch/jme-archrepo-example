package ch.admin.bit.jme.archrepo;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ArchRepoExampleIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8081/default-oauth-mock-server";
    private static final String SERVICE_BASE_URL = "http://localhost:8080/jme-archrepo-service";
    // Must match the system declared under archrepo.model.systems (jme-archrepo-service application.yml).
    // Since jeap-archrepo-service 18.0.0, systems/teams are no longer registered via a REST API
    // (the former POST /api/management/system was removed); they are declared through that
    // configuration instead and applied when a model update job runs.
    private static final String TEST_SYSTEM_NAME = "JME-Archrepo-Example";
    // Must match archrepo.api.username (application.yml) / archrepo.api.secret (application-local.yml)
    private static final String API_USER = "jme-archrepo-admin";
    private static final String API_SECRET = "secret";

    @BeforeAll
    static void startServices() throws Exception {
        startService("jme-archrepo-auth-scs", AUTH_BASE_URL);
        startService("jme-archrepo-service", SERVICE_BASE_URL);
    }

    @Test
    void triggerModelUpdateAndVerifyModel() {
        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic(API_USER, API_SECRET)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "type": "UPDATE_MODEL"
                        }
                        """)
                .when()
                .post("/api/jobs")
                .then()
                .statusCode(HttpStatus.OK.value());

        awaitModelContains(TEST_SYSTEM_NAME);
    }

    // The model update job runs asynchronously (@Async), so the configured system only shows up
    // in the model a short while after the POST /api/jobs call returns.
    private static void awaitModelContains(String expectedContent) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        String model;
        do {
            model = given()
                    .baseUri(SERVICE_BASE_URL)
                    .auth().preemptive().basic(API_USER, API_SECRET)
                    .when()
                    .get("/api/model")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().asString();
            if (model.contains(expectedContent)) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for model update", e);
            }
        } while (Instant.now().isBefore(deadline));
        assertThat(model).contains(expectedContent);
    }

    @Test
    void modelEndpointsShouldReturnSuccessfully() {
        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic(API_USER, API_SECRET)
                .when()
                .get("/api/model/rest-api-relation-without-pact")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic(API_USER, API_SECRET)
                .when()
                .get("/api/model/system-components-without-open-api-spec")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void shouldObtainAccessTokenFromAuthScs() {
        String accessToken = fetchAccessToken(AUTH_BASE_URL, "jme-archrepo-it-client", "secret");
        assertThat(accessToken).isNotBlank();
    }

}
