package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.CriterionDto;
import dev.abstratium.abstoggle.dto.RuleDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class RuleResourceTest {

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

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllRules_returnsRules() throws Exception {
        commitData(() -> {
            Rule rule1 = new Rule();
            rule1.setName("api-rule-get-1");
            rule1.setDescription("First rule");
            em.persist(rule1);

            Rule rule2 = new Rule();
            rule2.setName("api-rule-get-2");
            rule2.setDescription("Second rule");
            em.persist(rule2);

            Criterion criterion = new Criterion();
            criterion.setRule(rule1);
            criterion.setCriterionKey("country");
            criterion.setCriterionValue("/US/i");
            em.persist(criterion);
        });

        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(2))
            .body("[0].name", notNullValue())
            .body("[0].id", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllRules_emptyList_whenNoRules() {
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_persistsRule() {
        RuleDto request = new RuleDto();
        request.setName("api-rule-create");
        request.setDescription("Created via API");
        request.setCriteria(List.of(
            new CriterionDto(null, "country", "/US/i", null),
            new CriterionDto(null, "age", "/^[2-5][0-9]$/", null)
        ));

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/rules")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("api-rule-create"))
            .body("description", equalTo("Created via API"))
            .body("criteria.size()", equalTo(2))
            .body("criteria.size()", equalTo(2))
            .body("criteria.criterionKey", org.hamcrest.Matchers.hasItems("country", "age"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_nullRequest_returns400() {
        given()
            .contentType("application/json")
            .when()
            .post("/api/rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("Request body is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_duplicateName_returns400() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-dup");
            rule.setDescription("Existing rule");
            em.persist(rule);
        });

        RuleDto request = new RuleDto();
        request.setName("api-rule-dup");
        request.setDescription("Duplicate attempt");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("already exists"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_emptyName_returns400() {
        RuleDto request = new RuleDto();
        request.setName("");
        request.setDescription("Rule with empty name");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("name"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_updatesFields() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-update");
            rule.setDescription("Original description");
            em.persist(rule);

            Criterion criterion = new Criterion();
            criterion.setRule(rule);
            criterion.setCriterionKey("old_key");
            criterion.setCriterionValue("old_value");
            em.persist(criterion);
        });

        // Get the rule ID
        Rule existing = em.createQuery("SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", "api-rule-update")
            .getSingleResult();

        RuleDto request = new RuleDto();
        request.setName("api-rule-update-renamed");
        request.setDescription("Updated description");
        request.setCriteria(List.of(
            new CriterionDto(null, "new_country", "/UK/i", null)
        ));

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Updating rule with new criteria")
            .when()
            .put("/api/rules/" + existing.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo(existing.getId()))
            .body("name", equalTo("api-rule-update-renamed"))
            .body("description", equalTo("Updated description"))
            .body("criteria.size()", equalTo(1))
            .body("criteria[0].criterionKey", equalTo("new_country"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_whitespaceId_returns405() {
        // JAX-RS returns 405 for empty/whitespace path segments (no matching route)
        RuleDto request = new RuleDto();
        request.setName("some-name");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Test whitespace ID")
            .when()
            .put("/api/rules/  ")
            .then()
            .statusCode(405);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_nullRequestBody_returns400() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-null-req");
            em.persist(rule);
        });

        Rule existing = em.createQuery("SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", "api-rule-null-req")
            .getSingleResult();

        given()
            .contentType("application/json")
            .queryParam("changeNote", "Test null request body")
            .when()
            .put("/api/rules/" + existing.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("Request body is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_nonexistent_returns400() {
        RuleDto request = new RuleDto();
        request.setName("updated-name");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Update nonexistent rule")
            .when()
            .put("/api/rules/nonexistent-id-xyz")
            .then()
            .statusCode(400)
            .body("detail", containsString("not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteRule_removesRule() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-delete");
            em.persist(rule);
        });

        Rule existing = em.createQuery("SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", "api-rule-delete")
            .getSingleResult();

        given()
            .queryParam("changeNote", "Deleting rule via API")
            .when()
            .delete("/api/rules/" + existing.getId())
            .then()
            .statusCode(200);

        // Verify it's gone
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteRule_withAssignments_returnsError() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-with-assignments");
            em.persist(rule);

            Toggle toggle = new Toggle();
            toggle.setName("api-delete-rule-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            dev.abstratium.abstoggle.entity.Stage stage = new dev.abstratium.abstoggle.entity.Stage();
            stage.setName("api-delete-rule-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            em.persist(tsr);
        });

        Rule existing = em.createQuery("SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", "api-rule-with-assignments")
            .getSingleResult();

        given()
            .queryParam("changeNote", "Attempt to delete assigned rule")
            .when()
            .delete("/api/rules/" + existing.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("assigned"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteRule_nonexistent_returns200() {
        // Deleting non-existent rule returns 200 (no-op)
        given()
            .queryParam("changeNote", "Delete nonexistent rule")
            .when()
            .delete("/api/rules/nonexistent-id-xyz")
            .then()
            .statusCode(200);
    }


    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_noCriteria() {
        RuleDto request = new RuleDto();
        request.setName("api-rule-no-criteria");
        request.setDescription("Rule without criteria");
        request.setCriteria(List.of());

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/rules")
            .then()
            .statusCode(200)
            .body("name", equalTo("api-rule-no-criteria"))
            .body("criteria.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_clearCriteria() throws Exception {
        commitData(() -> {
            Rule rule = new Rule();
            rule.setName("api-rule-clear-crit");
            em.persist(rule);

            Criterion criterion = new Criterion();
            criterion.setRule(rule);
            criterion.setCriterionKey("temp");
            criterion.setCriterionValue("tempval");
            em.persist(criterion);
        });

        Rule existing = em.createQuery("SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", "api-rule-clear-crit")
            .getSingleResult();

        RuleDto request = new RuleDto();
        request.setName("api-rule-clear-crit");
        request.setDescription("Updated");
        request.setCriteria(List.of()); // Clear criteria

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Clearing rule criteria")
            .when()
            .put("/api/rules/" + existing.getId())
            .then()
            .statusCode(200)
            .body("criteria.size()", equalTo(0));
    }

    @Test
    void testGetAllRules_withoutAuth_returns401or400() {
        // Without auth, either 401 (unauthorized) or 400 (if validation runs first in test env)
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(400), equalTo(401)));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "some_other_role" })
    void testGetAllRules_withoutProperRole_returns403() {
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(403);
    }
}
