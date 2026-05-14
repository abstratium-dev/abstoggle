package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class ToggleStageRuleResourceTest {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction userTransaction;

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
    void testGetStageRulesForToggle_returnsAssignments() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-test-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-test-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-test-rule");
            rule.setRuleValue("on");
            rule.setDescription("Test rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(50);
            em.persist(tsr);
        });

        given()
            .when()
            .get("/api/toggles/api-test-toggle/stage-rules")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("size()", is(1))
            .body("[0].id", notNullValue())
            .body("[0].toggleName", is("api-test-toggle"))
            .body("[0].stageName", is("api-test-stage"))
            .body("[0].ruleName", is("api-test-rule"))
            .body("[0].ruleValue", is("on"))
            .body("[0].priority", is(50));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetStageRulesForToggle_returnsEmptyListWhenNoAssignments() {
        given()
            .when()
            .get("/api/toggles/empty-toggle/stage-rules")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStageRule_createsAssignment() throws Exception {
        final String[] ruleIdHolder = new String[1];
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-create-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-create-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-create-rule");
            rule.setRuleValue("beta");
            em.persist(rule);
            ruleIdHolder[0] = rule.getId();
        });

        given()
            .contentType("application/json")
            .body("{\"stageName\":\"api-create-stage\",\"ruleId\":\"" + ruleIdHolder[0] + "\",\"priority\":25}")
            .when()
            .post("/api/toggles/api-create-toggle/stage-rules")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStageRule_missingStageName_returns400() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-missing-stage-toggle");
            toggle.setContext("global");
            em.persist(toggle);
        });

        given()
            .contentType("application/json")
            .body("{\"ruleId\":\"some-rule-id\",\"priority\":10}")
            .when()
            .post("/api/toggles/api-missing-stage-toggle/stage-rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStageRulePriority_updatesPriority() throws Exception {
        final String[] tsrIdHolder = new String[1];
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-update-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-update-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-update-rule");
            rule.setRuleValue("on");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
            tsrIdHolder[0] = tsr.getId();
        });

        given()
            .contentType("application/json")
            .body("{\"priority\":5}")
            .when()
            .put("/api/toggles/api-update-toggle/stage-rules/" + tsrIdHolder[0])
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("id", is(tsrIdHolder[0]))
            .body("priority", is(5));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteStageRule_removesAssignment() throws Exception {
        final String[] tsrIdHolder = new String[1];
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-delete-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-delete-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-delete-rule");
            rule.setRuleValue("off");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
            tsrIdHolder[0] = tsr.getId();
        });

        given()
            .when()
            .delete("/api/toggles/api-delete-toggle/stage-rules/" + tsrIdHolder[0])
            .then()
            .statusCode(200);

        // Verify it was deleted
        given()
            .when()
            .get("/api/toggles/api-delete-toggle/stage-rules")
            .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStageRule_toggleNotFound_returns400() {
        given()
            .contentType("application/json")
            .body("{\"stageName\":\"nonexistent-stage\",\"ruleId\":\"some-id\",\"priority\":10}")
            .when()
            .post("/api/toggles/nonexistent-toggle/stage-rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStageRule_stageNotFound_returns400() throws Exception {
        commitData(() -> {
            Toggle toggle = new Toggle();
            toggle.setName("api-no-stage-toggle");
            toggle.setContext("global");
            em.persist(toggle);
        });

        given()
            .contentType("application/json")
            .body("{\"stageName\":\"nonexistent-stage\",\"ruleId\":\"some-id\",\"priority\":10}")
            .when()
            .post("/api/toggles/api-no-stage-toggle/stage-rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStageRule_ruleNotFound_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-no-rule-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-no-rule-toggle");
            toggle.setContext("global");
            em.persist(toggle);
        });

        given()
            .contentType("application/json")
            .body("{\"stageName\":\"api-no-rule-stage\",\"ruleId\":\"nonexistent-rule-id\",\"priority\":10}")
            .when()
            .post("/api/toggles/api-no-rule-toggle/stage-rules")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStageRulePriority_assignmentNotFound_returns400() {
        given()
            .contentType("application/json")
            .body("{\"priority\":5}")
            .when()
            .put("/api/toggles/api-update-toggle/stage-rules/nonexistent-id")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteStageRule_assignmentNotFound_returns200() {
        given()
            .when()
            .delete("/api/toggles/api-delete-toggle/stage-rules/nonexistent-id")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testAssignmentsOrderedByStageAndPriority() throws Exception {
        commitData(() -> {
            Stage dev = new Stage();
            dev.setName("api-dev-ordered");
            dev.setDisplayOrder(1);
            em.persist(dev);

            Stage prod = new Stage();
            prod.setName("api-prod-ordered");
            prod.setDisplayOrder(2);
            em.persist(prod);

            Toggle toggle = new Toggle();
            toggle.setName("api-ordered-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule1 = new ToggleRule();
            rule1.setName("api-ordered-rule-1");
            rule1.setRuleValue("on");
            em.persist(rule1);

            ToggleRule rule2 = new ToggleRule();
            rule2.setName("api-ordered-rule-2");
            rule2.setRuleValue("off");
            em.persist(rule2);

            ToggleStageRule tsrProd = new ToggleStageRule();
            tsrProd.setToggle(toggle);
            tsrProd.setStage(prod);
            tsrProd.setRule(rule1);
            tsrProd.setPriority(100);
            em.persist(tsrProd);

            ToggleStageRule tsrDev = new ToggleStageRule();
            tsrDev.setToggle(toggle);
            tsrDev.setStage(dev);
            tsrDev.setRule(rule2);
            tsrDev.setPriority(50);
            em.persist(tsrDev);
        });

        given()
            .when()
            .get("/api/toggles/api-ordered-toggle/stage-rules")
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .body("[0].stageName", is("api-dev-ordered"))
            .body("[0].priority", is(50))
            .body("[1].stageName", is("api-prod-ordered"))
            .body("[1].priority", is(100));
    }
}
