package ch.admin.bit.jme.archrepo;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ArchRepoExampleIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8081/default-oauth-mock-server";
    private static final String SERVICE_BASE_URL = "http://localhost:8080/jme-archrepo-service";
    private static final String TEST_SYSTEM_NAME = "JME-IT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    @BeforeAll
    static void startServices() throws Exception {
        startService("jme-archrepo-auth-scs", AUTH_BASE_URL);
        startService("jme-archrepo-service", SERVICE_BASE_URL);
    }

    @Test
    void registerSystemAndVerifyModel() {
        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic("api", "secret")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "%s",
                          "description": "jEAP Examples",
                          "confluenceLink": "https://confluence.bit.admin.ch/display/JEAP",
                          "teamName": "jEAP"
                        }
                        """.formatted(TEST_SYSTEM_NAME))
                .when()
                .post("/api/management/system")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        String model = given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic("api", "secret")
                .when()
                .get("/api/model")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        assertThat(model).contains(TEST_SYSTEM_NAME);
    }

    @Test
    void modelEndpointsShouldReturnSuccessfully() {
        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic("api", "secret")
                .when()
                .get("/api/model/rest-api-relation-without-pact")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .baseUri(SERVICE_BASE_URL)
                .auth().preemptive().basic("api", "secret")
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
