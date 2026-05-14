package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
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
class ToggleResourceTest {

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
    void testDeleteToggle_withToggleStageRules_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-delete-toggle-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-delete-toggle-with-rules");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-delete-toggle-rule");
            rule.setRuleValue("on");
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
            .delete("/api/toggles/api-delete-toggle-with-rules")
            .then()
            .statusCode(400)
            .body("detail", org.hamcrest.CoreMatchers.containsString("Remove the rules first"));
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

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_filterByStage_returnsMatchingToggles() throws Exception {
        commitData(() -> {
            Stage stageDev = new Stage();
            stageDev.setName("filter-dev");
            stageDev.setDisplayOrder(1);
            em.persist(stageDev);

            Stage stageProd = new Stage();
            stageProd.setName("filter-prod");
            stageProd.setDisplayOrder(2);
            em.persist(stageProd);

            Toggle toggleA = new Toggle();
            toggleA.setName("filter-toggle-a");
            toggleA.setContext("global");
            em.persist(toggleA);

            Toggle toggleB = new Toggle();
            toggleB.setName("filter-toggle-b");
            toggleB.setContext("global");
            em.persist(toggleB);

            ToggleRule rule = new ToggleRule();
            rule.setName("filter-rule");
            rule.setRuleValue("on");
            em.persist(rule);

            // Assign toggleA to dev stage
            ToggleStageRule tsrA = new ToggleStageRule();
            tsrA.setToggle(toggleA);
            tsrA.setStage(stageDev);
            tsrA.setRule(rule);
            tsrA.setPriority(100);
            em.persist(tsrA);

            // Assign toggleB to prod stage
            ToggleStageRule tsrB = new ToggleStageRule();
            tsrB.setToggle(toggleB);
            tsrB.setStage(stageProd);
            tsrB.setRule(rule);
            tsrB.setPriority(100);
            em.persist(tsrB);
        });

        given()
            .when()
            .queryParam("assignedToStage", "filter-dev")
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.CoreMatchers.equalTo(1))
            .body("[0].name", org.hamcrest.CoreMatchers.equalTo("filter-toggle-a"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_filterByRule_returnsMatchingToggles() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("filter-rule-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggleA = new Toggle();
            toggleA.setName("filter-rule-toggle-a");
            toggleA.setContext("global");
            em.persist(toggleA);

            Toggle toggleB = new Toggle();
            toggleB.setName("filter-rule-toggle-b");
            toggleB.setContext("global");
            em.persist(toggleB);

            ToggleRule ruleEu = new ToggleRule();
            ruleEu.setName("eu-customers");
            ruleEu.setRuleValue("on");
            em.persist(ruleEu);

            ToggleRule ruleUs = new ToggleRule();
            ruleUs.setName("us-customers");
            ruleUs.setRuleValue("off");
            em.persist(ruleUs);

            ToggleStageRule tsrA = new ToggleStageRule();
            tsrA.setToggle(toggleA);
            tsrA.setStage(stage);
            tsrA.setRule(ruleEu);
            tsrA.setPriority(100);
            em.persist(tsrA);

            ToggleStageRule tsrB = new ToggleStageRule();
            tsrB.setToggle(toggleB);
            tsrB.setStage(stage);
            tsrB.setRule(ruleUs);
            tsrB.setPriority(100);
            em.persist(tsrB);
        });

        given()
            .when()
            .queryParam("assignedToRule", "eu-customers")
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.CoreMatchers.equalTo(1))
            .body("[0].name", org.hamcrest.CoreMatchers.equalTo("filter-rule-toggle-a"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_filterByStageAndRule_returnsMatchingToggles() throws Exception {
        commitData(() -> {
            Stage stageDev = new Stage();
            stageDev.setName("combined-dev");
            stageDev.setDisplayOrder(1);
            em.persist(stageDev);

            Stage stageProd = new Stage();
            stageProd.setName("combined-prod");
            stageProd.setDisplayOrder(2);
            em.persist(stageProd);

            Toggle toggleA = new Toggle();
            toggleA.setName("combined-toggle-a");
            toggleA.setContext("global");
            em.persist(toggleA);

            Toggle toggleB = new Toggle();
            toggleB.setName("combined-toggle-b");
            toggleB.setContext("global");
            em.persist(toggleB);

            ToggleRule ruleAlpha = new ToggleRule();
            ruleAlpha.setName("alpha-rule");
            ruleAlpha.setRuleValue("on");
            em.persist(ruleAlpha);

            ToggleRule ruleBeta = new ToggleRule();
            ruleBeta.setName("beta-rule");
            ruleBeta.setRuleValue("off");
            em.persist(ruleBeta);

            // toggleA: dev + alpha
            ToggleStageRule tsrA = new ToggleStageRule();
            tsrA.setToggle(toggleA);
            tsrA.setStage(stageDev);
            tsrA.setRule(ruleAlpha);
            tsrA.setPriority(100);
            em.persist(tsrA);

            // toggleB: prod + alpha (same rule, different stage)
            ToggleStageRule tsrB = new ToggleStageRule();
            tsrB.setToggle(toggleB);
            tsrB.setStage(stageProd);
            tsrB.setRule(ruleAlpha);
            tsrB.setPriority(100);
            em.persist(tsrB);

            // toggleA: prod + beta (different assignment for same toggle)
            ToggleStageRule tsrC = new ToggleStageRule();
            tsrC.setToggle(toggleA);
            tsrC.setStage(stageProd);
            tsrC.setRule(ruleBeta);
            tsrC.setPriority(100);
            em.persist(tsrC);
        });

        given()
            .when()
            .queryParam("assignedToStage", "combined-dev")
            .queryParam("assignedToRule", "alpha-rule")
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.CoreMatchers.equalTo(1))
            .body("[0].name", org.hamcrest.CoreMatchers.equalTo("combined-toggle-a"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllToggles_noFilters_returnsAllToggles() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("no-filter-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggleA = new Toggle();
            toggleA.setName("no-filter-a");
            toggleA.setContext("global");
            em.persist(toggleA);

            Toggle toggleB = new Toggle();
            toggleB.setName("no-filter-b");
            toggleB.setContext("global");
            em.persist(toggleB);

            ToggleRule rule = new ToggleRule();
            rule.setName("no-filter-rule");
            rule.setRuleValue("on");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggleA);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(100);
            em.persist(tsr);
        });

        given()
            .when()
            .get("/api/toggles/all")
            .then()
            .statusCode(200)
            .body("size()", Matchers.greaterThanOrEqualTo(2))
            .body("name", org.hamcrest.CoreMatchers.hasItems("no-filter-a", "no-filter-b"));
    }
}
