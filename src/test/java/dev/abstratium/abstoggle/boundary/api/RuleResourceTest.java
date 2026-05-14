package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
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
            em.createQuery("DELETE FROM ToggleCriterion").executeUpdate();
            em.createQuery("DELETE FROM ToggleStageRule").executeUpdate();
            em.createQuery("DELETE FROM ToggleRule").executeUpdate();
            em.createQuery("DELETE FROM Toggle WHERE name LIKE 'rule-test%'").executeUpdate();
            userTransaction.commit();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllRules_returnsRulesWithCriteria() throws Exception {
        // Create test rules in a committed transaction
        userTransaction.begin();
        try {
            ToggleRule rule1 = new ToggleRule();
            rule1.setName("rule-test-1");
            rule1.setRuleValue("on");
            rule1.setDescription("First test rule");
            em.persist(rule1);

            ToggleCriterion criterion1 = new ToggleCriterion();
            criterion1.setToggleRule(rule1);
            criterion1.setCriterionKey("country");
            criterion1.setCriterionValue("DE");
            em.persist(criterion1);

            ToggleRule rule2 = new ToggleRule();
            rule2.setName("rule-test-2");
            rule2.setRuleValue("off");
            rule2.setDescription("Second test rule");
            em.persist(rule2);

            em.flush();
            userTransaction.commit();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }

        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("size()", is(2))
            .body("[0].id", notNullValue())
            .body("[0].value", is("on"))
            .body("[0].description", is("First test rule"))
            .body("[0].criteria.country", is("DE"))
            .body("[1].value", is("off"))
            .body("[1].description", is("Second test rule"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllRules_returnsEmptyListWhenNoRules() {
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_createsStandaloneRule() {
        given()
            .contentType("application/json")
            .body("{\"name\":\"api-new-rule\",\"ruleValue\":\"variant-a\",\"description\":\"A test rule\",\"criteria\":{\"userId\":\"^alice$\"}}")
            .when()
            .post("/api/rules")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("id", notNullValue())
            .body("name", is("api-new-rule"))
            .body("value", is("variant-a"))
            .body("description", is("A test rule"))
            .body("criteria.userId", is("^alice$"));

        // Verify it appears in the list
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].name", is("api-new-rule"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_missingName_returns400() {
        given()
            .contentType("application/json")
            .body("{\"ruleValue\":\"on\",\"criteria\":{}}")
            .when()
            .post("/api/rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateRule_duplicateName_returns400() throws Exception {
        userTransaction.begin();
        try {
            ToggleRule rule = new ToggleRule();
            rule.setName("api-dup-rule");
            rule.setRuleValue("on");
            em.persist(rule);
            em.flush();
            userTransaction.commit();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }

        given()
            .contentType("application/json")
            .body("{\"name\":\"api-dup-rule\",\"ruleValue\":\"off\",\"criteria\":{}}")
            .when()
            .post("/api/rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_updatesRule() throws Exception {
        final String[] ruleIdHolder = new String[1];
        userTransaction.begin();
        try {
            ToggleRule rule = new ToggleRule();
            rule.setName("api-update-rule");
            rule.setRuleValue("on");
            rule.setDescription("Original desc");
            em.persist(rule);
            em.flush();
            userTransaction.commit();
            ruleIdHolder[0] = rule.getId();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }

        given()
            .contentType("application/json")
            .body("{\"name\":\"api-update-rule\",\"ruleValue\":\"off\",\"description\":\"Updated desc\",\"criteria\":{\"env\":\"prod\"}}")
            .when()
            .put("/api/rules/" + ruleIdHolder[0])
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("id", is(ruleIdHolder[0]))
            .body("name", is("api-update-rule"))
            .body("value", is("off"))
            .body("description", is("Updated desc"))
            .body("criteria.env", is("prod"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateRule_notFound_returns400() {
        given()
            .contentType("application/json")
            .body("{\"name\":\"nonexistent\",\"ruleValue\":\"on\",\"criteria\":{}}")
            .when()
            .put("/api/rules/nonexistent-id")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteRule_deletesStandaloneRule() throws Exception {
        final String[] ruleIdHolder = new String[1];
        userTransaction.begin();
        try {
            ToggleRule rule = new ToggleRule();
            rule.setName("api-delete-rule");
            rule.setRuleValue("on");
            em.persist(rule);
            em.flush();
            userTransaction.commit();
            ruleIdHolder[0] = rule.getId();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }

        given()
            .when()
            .delete("/api/rules/" + ruleIdHolder[0])
            .then()
            .statusCode(200);

        // Verify it was deleted
        given()
            .when()
            .get("/api/rules")
            .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteRule_stillAssigned_returns400() throws Exception {
        final String[] ruleIdHolder = new String[1];
        userTransaction.begin();
        try {
            dev.abstratium.abstoggle.entity.Stage stage = new dev.abstratium.abstoggle.entity.Stage();
            stage.setName("api-delete-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            dev.abstratium.abstoggle.entity.Toggle toggle = new dev.abstratium.abstoggle.entity.Toggle();
            toggle.setName("api-delete-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-assigned-rule");
            rule.setRuleValue("on");
            em.persist(rule);

            dev.abstratium.abstoggle.entity.ToggleStageRule tsr = new dev.abstratium.abstoggle.entity.ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);

            em.flush();
            userTransaction.commit();
            ruleIdHolder[0] = rule.getId();
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }

        given()
            .when()
            .delete("/api/rules/" + ruleIdHolder[0])
            .then()
            .statusCode(400);
    }
}
