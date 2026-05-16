package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class ToggleResourceTest {

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

    private void commitData(Runnable setup) throws Exception {
        userTransaction.begin();
        try {
            setup.run();
            em.flush();
            userTransaction.commit();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }
    }

    // ==================== QUERY TOGGLES ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_withStage_returnsToggles() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-query-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-query-toggle");
            toggle.setContext("global");
            toggle.setEnabled(true);
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-query-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
        });

        given()
            .when()
            .get("/api/toggles?stage=api-query-stage")
            .then()
            .statusCode(200)
            .body("toggles", notNullValue())
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("api-query-toggle"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_missingStage_returns400() {
        given()
            .when()
            .get("/api/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage parameter is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_withNameFilter_returnsFiltered() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-filter-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle1 = new Toggle();
            toggle1.setName("feature-alpha-test");
            toggle1.setContext("global");
            em.persist(toggle1);

            Toggle toggle2 = new Toggle();
            toggle2.setName("feature-beta-test");
            toggle2.setContext("global");
            em.persist(toggle2);

            Rule rule = new Rule();
            rule.setName("api-filter-rule");
            em.persist(rule);

            ToggleStageRule tsr1 = new ToggleStageRule();
            tsr1.setToggle(toggle1);
            tsr1.setStage(stage);
            tsr1.setRule(rule);
            tsr1.setPriority(100);
            em.persist(tsr1);

            ToggleStageRule tsr2 = new ToggleStageRule();
            tsr2.setToggle(toggle2);
            tsr2.setStage(stage);
            tsr2.setRule(rule);
            tsr2.setPriority(200);
            em.persist(tsr2);
        });

        given()
            .when()
            .get("/api/toggles?stage=api-filter-stage&nameFilter=%alpha%")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("feature-alpha-test"));
    }

    // ==================== GET ALL TOGGLES ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_returnsAllToggles() throws Exception {
        commitData(() -> {
            Toggle toggle1 = new Toggle();
            toggle1.setName("api-all-toggle-1");
            toggle1.setDescription("First toggle");
            toggle1.setEnabled(true);
            toggle1.setContext("ctx1");
            em.persist(toggle1);

            Toggle toggle2 = new Toggle();
            toggle2.setName("api-all-toggle-2");
            toggle2.setDescription("Second toggle");
            toggle2.setEnabled(false);
            toggle2.setContext("ctx2");
            em.persist(toggle2);
        });

        given()
            .when()
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(2))
            .body("[0].name", notNullValue())
            .body("[0].enabled", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_assignedToStage_returnsFiltered() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-assigned-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle assignedToggle = new Toggle();
            assignedToggle.setName("api-assigned-toggle");
            assignedToggle.setContext("global");
            em.persist(assignedToggle);

            Toggle unassignedToggle = new Toggle();
            unassignedToggle.setName("api-unassigned-toggle");
            unassignedToggle.setContext("global");
            em.persist(unassignedToggle);

            Rule rule = new Rule();
            rule.setName("api-assigned-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(assignedToggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
        });

        given()
            .when()
            .get("/api/toggles/all?assignedToStage=api-assigned-stage")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(1))
            .body("[0].name", equalTo("api-assigned-toggle"));
    }

    // ==================== CREATE TOGGLE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_persistsToggle() {
        ToggleDto request = new ToggleDto();
        request.setName("api-create-toggle");
        request.setDescription("Created via API");
        request.setEnabled(true);
        request.setContext("test-context");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("api-create-toggle"))
            .body("description", equalTo("Created via API"))
            .body("enabled", equalTo(true))
            .body("context", equalTo("test-context"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_missingName_returns400() {
        ToggleDto request = new ToggleDto();
        request.setDescription("Missing name");
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle name is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_nullName_returns400() {
        ToggleDto request = new ToggleDto();
        request.setName(null);
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle name is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_blankName_returns400() {
        ToggleDto request = new ToggleDto();
        request.setName("   ");
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle name is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_duplicateName_returns400() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-duplicate-toggle");
            toggle.setContext("global");
            em.persist(toggle);
        });

        ToggleDto request = new ToggleDto();
        request.setName("api-duplicate-toggle");
        request.setDescription("Duplicate");
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(409)
            .body("detail", containsString("already exists"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateToggle_defaultsEnabledAndContext() {
        ToggleDto request = new ToggleDto();
        request.setName("api-default-toggle");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(200)
            .body("enabled", equalTo(true))
            .body("context", equalTo(""));
    }

    // ==================== UPDATE TOGGLE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateToggle_updatesFields() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-update-toggle");
            toggle.setDescription("Original desc");
            toggle.setEnabled(true);
            toggle.setContext("original-ctx");
            em.persist(toggle);
        });

        Toggle existing = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-update-toggle")
            .getSingleResult();

        ToggleDto request = new ToggleDto();
        request.setName("api-update-toggle-renamed");
        request.setDescription("Updated desc");
        request.setEnabled(false);
        request.setContext("updated-ctx");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/" + existing.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("api-update-toggle-renamed"))
            .body("description", equalTo("Updated desc"))
            .body("enabled", equalTo(false))
            .body("context", equalTo("updated-ctx"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateToggle_nonexistent_returns400() {
        ToggleDto request = new ToggleDto();
        request.setName("nonexistent-toggle-updated");
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/nonexistent-id-12345")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateToggle_duplicateName_returns400() throws Exception {
        commitData(() -> {
            Toggle toggle1 = new Toggle();
            toggle1.setName("api-update-existing");
            toggle1.setContext("global");
            em.persist(toggle1);

            Toggle toggle2 = new Toggle();
            toggle2.setName("api-update-target");
            toggle2.setContext("global");
            em.persist(toggle2);
        });

        Toggle target = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-update-target")
            .getSingleResult();

        ToggleDto request = new ToggleDto();
        request.setName("api-update-existing");
        request.setEnabled(true);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/" + target.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("already exists"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateToggle_sameNameAllowed() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-same-name-toggle");
            toggle.setDescription("Original");
            toggle.setEnabled(true);
            toggle.setContext("ctx");
            em.persist(toggle);
        });

        Toggle existing = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-same-name-toggle")
            .getSingleResult();

        ToggleDto request = new ToggleDto();
        request.setName("api-same-name-toggle");
        request.setDescription("Updated description");
        request.setEnabled(false);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/" + existing.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("api-same-name-toggle"))
            .body("description", equalTo("Updated description"))
            .body("enabled", equalTo(false));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateToggle_partialUpdate() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-partial-toggle");
            toggle.setDescription("Original desc");
            toggle.setEnabled(true);
            toggle.setContext("original-ctx");
            em.persist(toggle);
        });

        Toggle existing = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-partial-toggle")
            .getSingleResult();

        ToggleDto request = new ToggleDto();
        request.setName("api-partial-toggle");
        request.setDescription("Updated desc only");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/" + existing.getId())
            .then()
            .statusCode(200)
            .body("name", equalTo("api-partial-toggle"))
            .body("description", equalTo("Updated desc only"))
            .body("enabled", equalTo(true))
            .body("context", equalTo("original-ctx"));
    }

    // ==================== DELETE TOGGLE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteToggle_removesToggle() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-delete-toggle");
            toggle.setContext("global");
            em.persist(toggle);
        });

        Toggle existing = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-delete-toggle")
            .getSingleResult();

        given()
            .when()
            .delete("/api/toggles/" + existing.getId())
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteToggle_withAssignments_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-delete-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-delete-with-assignments");
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-delete-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
        });

        Toggle existing = em.createQuery("SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", "api-delete-with-assignments")
            .getSingleResult();

        given()
            .when()
            .delete("/api/toggles/" + existing.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("Remove the rules first"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteToggle_nonexistent_returns200() {
        given()
            .when()
            .delete("/api/toggles/nonexistent-toggle-xyz")
            .then()
            .statusCode(200);
    }

    // ==================== SECURITY ====================

    @Test
    void testQueryToggles_withoutAuth_returns401or400or302() {
        given()
            .redirects().follow(false)
            .when()
            .get("/api/toggles?stage=test")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    void testGetAllToggles_withoutAuth_returns401or400or302() {
        given()
            .redirects().follow(false)
            .when()
            .get("/api/toggles/all")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    void testCreateToggle_withoutAuth_returns401or400or302() {
        ToggleDto request = new ToggleDto();
        request.setName("unauthorized-toggle");

        given()
            .redirects().follow(false)
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/toggles")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    void testUpdateToggle_withoutAuth_returns401or400or302() {
        ToggleDto request = new ToggleDto();
        request.setName("unauthorized-toggle");

        given()
            .redirects().follow(false)
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/toggles/some-id")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    void testDeleteToggle_withoutAuth_returns401or400or302() {
        given()
            .redirects().follow(false)
            .when()
            .delete("/api/toggles/some-id")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "some_other_role" })
    void testQueryToggles_withoutProperRole_returns403() {
        given()
            .when()
            .get("/api/toggles?stage=test")
            .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "some_other_role" })
    void testGetAllToggles_withoutProperRole_returns403() {
        given()
            .when()
            .get("/api/toggles/all")
            .then()
            .statusCode(403);
    }
}
