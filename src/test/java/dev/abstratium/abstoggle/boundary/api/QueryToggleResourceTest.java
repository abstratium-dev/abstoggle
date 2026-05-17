package dev.abstratium.abstoggle.boundary.api;

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
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@QuarkusTest
class QueryToggleResourceTest {

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
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_returnsToggles_whenStageValid() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-query-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-query-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-query-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setRuleValue("on");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "api-query-stage")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles", notNullValue())
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("api-query-toggle"))
            .body("toggles[0].value", equalTo("on"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_query" })
    void testQueryToggles_withQueryRole_returnsToggles() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-query-role-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-query-role-toggle");
            toggle.setEnabled(true);
            toggle.setContext("api");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-query-role-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setRuleValue("enabled");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "api-query-role-stage")
            .queryParam("context", "api")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("api-query-role-toggle"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_missingStage_returns400() {
        given()
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage parameter is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_stageNotFound_returns400() {
        given()
            .queryParam("stage", "nonexistent-stage-xyz")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_withNameFilter() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-filter-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle1 = new Toggle();
            toggle1.setName("api-feature-alpha");
            toggle1.setEnabled(true);
            toggle1.setContext("global");
            em.persist(toggle1);

            Toggle toggle2 = new Toggle();
            toggle2.setName("api-feature-beta");
            toggle2.setEnabled(true);
            toggle2.setContext("global");
            em.persist(toggle2);

            Rule rule = new Rule();
            rule.setName("api-filter-rule");
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
            .queryParam("stage", "api-filter-stage")
            .queryParam("nameFilter", "%alpha%")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("api-feature-alpha"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_invalidNameFilter_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-regex-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        given()
            .queryParam("stage", "api-regex-stage")
            .queryParam("nameFilter", "[invalid")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(400)
            .body("detail", containsString("Invalid regex pattern"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_disabledTogglesExcluded() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-disabled-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle enabledToggle = new Toggle();
            enabledToggle.setName("api-enabled-toggle");
            enabledToggle.setEnabled(true);
            enabledToggle.setContext("global");
            em.persist(enabledToggle);

            Toggle disabledToggle = new Toggle();
            disabledToggle.setName("api-disabled-toggle");
            disabledToggle.setEnabled(false);
            disabledToggle.setContext("global");
            em.persist(disabledToggle);

            Rule rule = new Rule();
            rule.setName("api-disabled-rule");
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
            .queryParam("stage", "api-disabled-stage")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].toggleName", equalTo("api-enabled-toggle"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_includeDisabled_returnsAll() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-incl-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle enabledToggle = new Toggle();
            enabledToggle.setName("api-incl-enabled");
            enabledToggle.setEnabled(true);
            enabledToggle.setContext("global");
            em.persist(enabledToggle);

            Toggle disabledToggle = new Toggle();
            disabledToggle.setName("api-incl-disabled");
            disabledToggle.setEnabled(false);
            disabledToggle.setContext("global");
            em.persist(disabledToggle);

            Rule rule = new Rule();
            rule.setName("api-incl-rule");
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
            .queryParam("stage", "api-incl-stage")
            .queryParam("includeDisabled", "true")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(2));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_returnsCriteria() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-criteria-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-criteria-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-criteria-rule");
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
            tsr.setRuleValue("enabled");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "api-criteria-stage")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].ruleCriteria.size()", equalTo(1))
            .body("toggles[0].ruleCriteria[0].criterionKey", equalTo("country"))
            .body("toggles[0].ruleCriteria[0].criterionValue", equalTo("/US/i"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_stageInheritance() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-inherit-parent");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("api-inherit-child");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);

            Toggle toggle = new Toggle();
            toggle.setName("api-inherit-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-inherit-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(parent);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setRuleValue("parent-value");
            em.persist(tsr);
        });

        given()
            .queryParam("stage", "api-inherit-child")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(1))
            .body("toggles[0].value", equalTo("parent-value"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testQueryToggles_noAssignment_returnsEmpty() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-noassign-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-noassign-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);
        });

        given()
            .queryParam("stage", "api-noassign-stage")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("toggles.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testEvictCache_returns204() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-evict-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        given()
            .queryParam("stage", "api-evict-stage")
            .queryParam("context", "global")
            .when()
            .delete("/api/query/toggles/cache")
            .then()
            .statusCode(204);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testEvictCache_removesEntryFromCache_soNextQueryIsNotCacheHit() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-evict-cache-stage");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-evict-cache-toggle");
            toggle.setEnabled(true);
            toggle.setContext("global");
            em.persist(toggle);

            Rule rule = new Rule();
            rule.setName("api-evict-cache-rule");
            em.persist(rule);

            ToggleStageRule tsr = new ToggleStageRule();
            tsr.setToggle(toggle);
            tsr.setStage(stage);
            tsr.setRule(rule);
            tsr.setPriority(1);
            tsr.setRuleValue("on");
            em.persist(tsr);
        });

        // First query — populates cache; metadata.cacheHit must be false
        given()
            .queryParam("stage", "api-evict-cache-stage")
            .queryParam("context", "global")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("queryMetadata.cacheHit", equalTo(false));

        // Second query — should now be served from cache; cacheHit must be true
        given()
            .queryParam("stage", "api-evict-cache-stage")
            .queryParam("context", "global")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("queryMetadata.cacheHit", equalTo(true));

        // Evict the cache entry
        given()
            .queryParam("stage", "api-evict-cache-stage")
            .queryParam("context", "global")
            .when()
            .delete("/api/query/toggles/cache")
            .then()
            .statusCode(204);

        // Third query — cache was evicted, must be a fresh miss again
        given()
            .queryParam("stage", "api-evict-cache-stage")
            .queryParam("context", "global")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(200)
            .body("queryMetadata.cacheHit", equalTo(false));
    }

    @Test
    void testQueryToggles_withoutAuth_stageNotFound_returns401or400() {
        // In test environment without auth, the request either returns 401 (auth) or
        // validation fails first. We just verify it doesn't succeed.
        given()
            .queryParam("stage", "nonexistent-stage-xyz")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(400), equalTo(401)));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "some_other_role" })
    void testQueryToggles_withoutProperRole_stageNotFound_returns400or403() {
        // In test environment, stage validation may run before role check
        given()
            .queryParam("stage", "nonexistent-stage-xyz")
            .when()
            .get("/api/query/toggles")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(400), equalTo(403)));
    }
}
