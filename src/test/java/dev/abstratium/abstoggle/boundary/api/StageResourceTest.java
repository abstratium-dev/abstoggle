package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;

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
class StageResourceTest {

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
    void testDeleteStage_withToggleStageRules_returns400() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-delete-stage-with-rules");
            stage.setDisplayOrder(1);
            em.persist(stage);

            Toggle toggle = new Toggle();
            toggle.setName("api-delete-toggle");
            toggle.setContext("global");
            em.persist(toggle);

            ToggleRule rule = new ToggleRule();
            rule.setName("api-delete-rule");
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
            .delete("/api/stages/api-delete-stage-with-rules")
            .then()
            .statusCode(400)
            .body("detail", org.hamcrest.CoreMatchers.containsString("Remove the rules first"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteStage_withChildStage_returns400() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-parent-stage");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("api-child-stage");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);
        });

        given()
            .when()
            .delete("/api/stages/api-parent-stage")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteStage_nonexistent_returns200() {
        given()
            .when()
            .delete("/api/stages/nonexistent-stage-xyz")
            .then()
            .statusCode(200);
    }
}
