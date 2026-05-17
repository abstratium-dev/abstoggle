package dev.abstratium.abstoggle.boundary.interceptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.Roles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class ChangeNoteInterceptorTest {

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testRequiresChangeNote_queryParamPresent_allowsRequest() {
        given()
            .when()
            .post("/api/test-change-note?changeNote=query+param+note")
            .then()
            .statusCode(200)
            .body(equalTo("success"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testRequiresChangeNote_queryParamMissing_returns400() {
        given()
            .when()
            .post("/api/test-change-note")
            .then()
            .statusCode(400)
            .body("detail", containsString("Missing required query parameter: changeNote"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testRequiresChangeNote_queryParamEmpty_returns400() {
        given()
            .when()
            .post("/api/test-change-note?changeNote=")
            .then()
            .statusCode(400)
            .body("detail", containsString("Missing required query parameter: changeNote"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testRequiresChangeNote_queryParamBlank_returns400() {
        given()
            .when()
            .post("/api/test-change-note?changeNote=   ")
            .then()
            .statusCode(400)
            .body("detail", containsString("Missing required query parameter: changeNote"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testRequiresChangeNote_queryParamWithValue_allowsRequest() {
        given()
            .when()
            .post("/api/test-change-note?changeNote=Implementing+dark+mode+toggle")
            .then()
            .statusCode(200)
            .body(equalTo("success"));
    }
}
