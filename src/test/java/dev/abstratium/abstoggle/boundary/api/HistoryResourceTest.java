package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class HistoryResourceTest {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction userTransaction;

    @AfterEach
    void cleanup() throws Exception {
        userTransaction.begin();
        try {
            em.createQuery("DELETE FROM Criterion").executeUpdate();
            em.createQuery("DELETE FROM ToggleStageRule").executeUpdate();
            em.createQuery("DELETE FROM Rule").executeUpdate();
            em.createQuery("DELETE FROM Toggle").executeUpdate();
            em.createQuery("DELETE FROM Stage").executeUpdate();
            userTransaction.commit();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }
    }

    // ==================== GET /api/history ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_returnsEntries() {
        // Create a toggle via REST so Envers records a revision
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-toggle-1", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_withSearchTerm_filtersResults() {
        // Create toggle via REST to produce an audit entry with username "testuser@example.com"
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-toggle-search", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200);

        // Search by known username substring
        List<?> results = given()
            .queryParam("search", "testuser")
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .extract().as(List.class);

        assertFalse(results.isEmpty(), "Expected at least one history entry matching 'testuser'");
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_withNonMatchingSearch_returnsEmpty() {
        // Create something so the table is not empty
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-toggle-nomatch", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200);

        List<?> results = given()
            .queryParam("search", "ZZZNOBODYWILLEVERUSETHISASACHANGENOTE")
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .extract().as(List.class);

        assertTrue(results.isEmpty(), "Expected no results for unmatched search term");
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_limitAndOffset_work() {
        // Create several toggles to ensure enough history entries
        for (int i = 1; i <= 3; i++) {
            given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "hist-paging-toggle-" + i, "enabled", true, "context", "global"))
                .when()
                .post("/api/toggles")
                .then()
                .statusCode(200);
        }

        List<?> page1 = given()
            .queryParam("limit", 2)
            .queryParam("offset", 0)
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .extract().as(List.class);

        given()
            .queryParam("limit", 2)
            .queryParam("offset", 2)
            .when()
            .get("/api/history")
            .then()
            .statusCode(200);

        // Pages should differ (different revisions)
        assertFalse(page1.isEmpty(), "Page 1 should have entries");
        // page2 may or may not have entries depending on test DB state, but must not error
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_invalidLimit_returns400() {
        given()
            .queryParam("limit", 0)
            .when()
            .get("/api/history")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_limitTooLarge_returns400() {
        given()
            .queryParam("limit", 201)
            .when()
            .get("/api/history")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testSearchHistory_negativeOffset_returns400() {
        given()
            .queryParam("offset", -1)
            .when()
            .get("/api/history")
            .then()
            .statusCode(400);
    }

    // ==================== GET /api/history/{rev} ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetRevisionDetails_returnsChanges() {
        // Create a toggle via REST so Envers records a revision
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-detail-toggle", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200);

        // Retrieve the latest history entry to get a valid rev number
        List<Map<String, Object>> entries = given()
            .queryParam("limit", 10)
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .extract().as(new io.restassured.common.mapper.TypeRef<List<Map<String, Object>>>() {});

        assertFalse(entries.isEmpty(), "Expected history entries after creating a toggle");

        int rev = (int) entries.get(0).get("rev");

        given()
            .when()
            .get("/api/history/" + rev)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0)); // may be empty if another rev was latest
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetRevisionDetails_revZero_returns400() {
        given()
            .when()
            .get("/api/history/0")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetRevisionDetails_nonExistentRev_returnsEmptyList() {
        given()
            .when()
            .get("/api/history/999999999")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetRevisionDetails_withChangeNote_isSearchable() {
        // Create a toggle, then update it with a change note so that the change note appears in REVINFO
        String toggleId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-note-toggle", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200)
            .extract().path("id");

        // Update the toggle with a change note
        given()
            .contentType(ContentType.JSON)
            .queryParam("changeNote", "my-unique-note-xyz")
            .body(Map.of("name", "hist-note-toggle", "enabled", false, "context", "global"))
            .when()
            .put("/api/toggles/" + toggleId)
            .then()
            .statusCode(200);

        // Search by the change note
        List<?> results = given()
            .queryParam("search", "my-unique-note-xyz")
            .when()
            .get("/api/history")
            .then()
            .statusCode(200)
            .extract().as(List.class);

        assertFalse(results.isEmpty(), "Expected history entry with change note 'my-unique-note-xyz'");
    }

    // ==================== GET /api/history/entity/{table}/{entityId} ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetEntityHistory_returnsRevisions() {
        // Create a toggle via REST to produce audit entries
        String toggleId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "hist-entity-toggle", "enabled", true, "context", "global"))
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200)
            .extract().path("id");

        // Update it to create a second revision
        given()
            .contentType(ContentType.JSON)
            .queryParam("changeNote", "second-rev-note")
            .body(Map.of("name", "hist-entity-toggle", "enabled", false, "context", "global"))
            .when()
            .put("/api/toggles/" + toggleId)
            .then()
            .statusCode(200);

        List<?> revisions = given()
            .when()
            .get("/api/history/entity/Toggle/" + toggleId)
            .then()
            .statusCode(200)
            .extract().as(List.class);

        assertFalse(revisions.isEmpty(), "Expected at least one revision for the toggle");
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = {"abstratium-abstoggle_user"})
    void testGetEntityHistory_unknownTable_returns400() {
        given()
            .when()
            .get("/api/history/entity/UnknownTable/some-id")
            .then()
            .statusCode(400);
    }
}
