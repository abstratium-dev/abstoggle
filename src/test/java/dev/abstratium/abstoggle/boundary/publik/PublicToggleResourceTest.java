package dev.abstratium.abstoggle.boundary.publik;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class PublicToggleResourceTest {

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

    @Test
    void testQueryToggles_returnsToggles_whenStageAndContextValid() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-test-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("public-test-toggle");
            toggle.setEnabled(true);
            toggle.setContext("mobile");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("public-test-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setToggleValue("on");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "public-test-stage")
            .queryParam("context", "mobile")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles", notNullValue())
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("public-test-toggle"))
            .body("toggles[0].value", equalTo("on"));
    }

    @Test
    void testQueryToggles_missingStage_returns400() {
        given()
            .queryParam("context", "mobile")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage parameter is required"));
    }

    @Test
    void testQueryToggles_missingContext_returns400() {
        given()
            .queryParam("stage", "some-stage")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Context parameter is required"));
    }

    @Test
    void testQueryToggles_stageNotFound_returns400() {
        given()
            .queryParam("stage", "nonexistent-stage-xyz")
            .queryParam("context", "mobile")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage not found"));
    }

    @Test
    void testQueryToggles_withNameFilter() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-filter-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle1 = new Toggle();
            toggle1.setName("public-feature-alpha");
            toggle1.setEnabled(true);
            toggle1.setContext("global");
            em.persist(toggle1);

            Toggle toggle2 = new Toggle();
            toggle2.setName("public-feature-beta");
            toggle2.setEnabled(true);
            toggle2.setContext("global");
            em.persist(toggle2);

            Rule rule = new Rule();
            rule.setName("public-filter-rule");
            em.persist(rule);

            ToggleStageRule tsr1 = new ToggleStageRule();
            tsr1.setToggle(toggle1);
            tsr1.setStage(stage);
            tsr1.setRule(rule);
            tsr1.setPriority(1);
            em.persist(tsr1);

            ToggleStageRule tsr2 = new ToggleStageRule();
            tsr2.setToggle(toggle2);
            tsr2.setStage(stage);
            tsr2.setRule(rule);
            tsr2.setPriority(2);
            em.persist(tsr2);
        });

        given()
            .queryParam("stage", "public-filter-stage")
            .queryParam("context", "global")
            .queryParam("nameFilter", "%alpha%")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("public-feature-alpha"));
    }

    @Test
    void testQueryToggles_invalidNameFilter_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-regex-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        given()
            .queryParam("stage", "public-regex-stage")
            .queryParam("context", "mobile")
            .queryParam("nameFilter", "[invalid")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Invalid regex pattern"));
    }

    @Test
    void testQueryToggles_disabledTogglesExcluded() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-disabled-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle enabledToggle = new Toggle();
            enabledToggle.setName("public-enabled-toggle");
            enabledToggle.setEnabled(true);
            enabledToggle.setContext("global");
            em.persist(enabledToggle);

            Toggle disabledToggle = new Toggle();
            disabledToggle.setName("public-disabled-toggle");
            disabledToggle.setEnabled(false);
            disabledToggle.setContext("global");
            em.persist(disabledToggle);

            Rule rule = new Rule();
            rule.setName("public-disabled-rule");
            em.persist(rule);

            ToggleStageRule tsr1 = new ToggleStageRule();
            tsr1.setToggle(enabledToggle);
            tsr1.setStage(stage);
            tsr1.setRule(rule);
            tsr1.setPriority(1);
            em.persist(tsr1);

            ToggleStageRule tsr2 = new ToggleStageRule();
            tsr2.setToggle(disabledToggle);
            tsr2.setStage(stage);
            tsr2.setRule(rule);
            tsr2.setPriority(2);
            em.persist(tsr2);
        });

        given()
            .queryParam("stage", "public-disabled-stage")
            .queryParam("context", "global")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("public-enabled-toggle"));
    }

    @Test
    void testQueryToggles_includeDisabled_returnsAll() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-incl-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle enabledToggle = new Toggle();
            enabledToggle.setName("public-incl-enabled");
            enabledToggle.setEnabled(true);
            enabledToggle.setContext("global");
            em.persist(enabledToggle);

            Toggle disabledToggle = new Toggle();
            disabledToggle.setName("public-incl-disabled");
            disabledToggle.setEnabled(false);
            disabledToggle.setContext("global");
            em.persist(disabledToggle);

            Rule rule = new Rule();
            rule.setName("public-incl-rule");
            em.persist(rule);

            ToggleStageRule tsr1 = new ToggleStageRule();
            tsr1.setToggle(enabledToggle);
            tsr1.setStage(stage);
            tsr1.setRule(rule);
            tsr1.setPriority(1);
            em.persist(tsr1);

            ToggleStageRule tsr2 = new ToggleStageRule();
            tsr2.setToggle(disabledToggle);
            tsr2.setStage(stage);
            tsr2.setRule(rule);
            tsr2.setPriority(2);
            em.persist(tsr2);
        });

        given()
            .queryParam("stage", "public-incl-stage")
            .queryParam("context", "global")
            .queryParam("includeDisabled", "true")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(2));
    }

    @Test
    void testQueryToggles_returnsCriteria() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-criteria-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("public-criteria-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("public-criteria-rule");
            em.persist(rule);

            Criterion criterion = new Criterion();
            criterion.setRule(rule);
            criterion.setCriterionKey("country");
            criterion.setCriterionValue("/US/i");
            em.persist(criterion);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setToggleValue("enabled");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "public-criteria-stage")
            .queryParam("context", "global")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].ruleCriteria.size()", equalTo(1))
            .body("toggles[0].ruleCriteria[0].criterionKey", equalTo("country"))
            .body("toggles[0].ruleCriteria[0].criterionValue", equalTo("/US/i"));
    }

    @Test
    void testQueryToggles_stageInheritance() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("public-inherit-parent");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("public-inherit-child");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);

            Toggle toggle = new Toggle();
            toggle.setName("public-inherit-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("public-inherit-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(parent);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setToggleValue("parent-value");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "public-inherit-child")
            .queryParam("context", "global")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].value", equalTo("parent-value"));
    }

    @Test
    void testQueryToggles_noAssignment_returnsEmpty() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("public-noassign-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("public-noassign-toggle");
            toggle.setEnabled(true);
            em.persist(toggle);
        });

        given()
            .queryParam("stage", "public-noassign-stage")
            .queryParam("context", "global")
            .when()
            .get("/public/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(0));
    }
}
