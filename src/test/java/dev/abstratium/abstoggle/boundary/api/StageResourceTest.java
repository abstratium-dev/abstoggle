package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.StageDto;
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
class StageResourceTest {

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

    // ==================== GET ALL STAGES ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllStages_returnsStages() throws Exception {
        commitData(() -> {
            Stage stage1 = new Stage();
            stage1.setName("api-stage-get-1");
            stage1.setDescription("First stage");
            stage1.setDisplayOrder(1);
            em.persist(stage1);

            Stage stage2 = new Stage();
            stage2.setName("api-stage-get-2");
            stage2.setDescription("Second stage");
            stage2.setDisplayOrder(2);
            em.persist(stage2);
        });

        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(2))
            .body("[0].name", notNullValue())
            .body("[0].id", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllStages_emptyList_whenNoStages() {
        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testGetAllStages_returnsParentStageName() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-stage-parent");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("api-stage-child");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);
        });

        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(2))
            .body("find { it.name == 'api-stage-child' }.parentStageName", equalTo("api-stage-parent"));
    }

    // ==================== CREATE STAGE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_persistsStage() {
        StageDto request = new StageDto();
        request.setName("api-stage-create");
        request.setDescription("Created via API");
        request.setDisplayOrder(10);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("api-stage-create"))
            .body("description", equalTo("Created via API"))
            .body("displayOrder", equalTo(10));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_nullName_returns400() {
        StageDto request = new StageDto();
        request.setDescription("Stage with null name");
        request.setDisplayOrder(1);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(400)
            .body("detail", containsString("name is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_emptyName_returns400() {
        StageDto request = new StageDto();
        request.setName("   ");
        request.setDescription("Stage with empty name");
        request.setDisplayOrder(1);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(400)
            .body("detail", containsString("name is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_duplicateName_returns409() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-stage-dup");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        StageDto request = new StageDto();
        request.setName("api-stage-dup");
        request.setDescription("Duplicate name attempt");
        request.setDisplayOrder(2);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(409)
            .body("title", containsString("Duplicate"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_withParentStage() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-stage-parent-for-create");
            parent.setDisplayOrder(1);
            em.persist(parent);
        });

        StageDto request = new StageDto();
        request.setName("api-stage-with-parent");
        request.setDescription("Child stage");
        request.setDisplayOrder(2);
        request.setParentStageName("api-stage-parent-for-create");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(200)
            .body("name", equalTo("api-stage-with-parent"))
            .body("parentStageName", equalTo("api-stage-parent-for-create"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testCreateStage_withNonexistentParent_returns400() {
        StageDto request = new StageDto();
        request.setName("api-stage-bad-parent");
        request.setDescription("Stage with bad parent");
        request.setDisplayOrder(1);
        request.setParentStageName("nonexistent-parent-stage");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/stages")
            .then()
            .statusCode(400)
            .body("detail", containsString("not found"));
    }

    // ==================== UPDATE STAGE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStage_updatesFields() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-stage-update");
            stage.setDescription("Original description");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        Stage existing = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-stage-update")
            .getSingleResult();

        StageDto request = new StageDto();
        request.setName("api-stage-update-renamed");
        request.setDescription("Updated description");
        request.setDisplayOrder(99);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/stages/" + existing.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo(existing.getId()))
            .body("name", equalTo("api-stage-update-renamed"))
            .body("description", equalTo("Updated description"))
            .body("displayOrder", equalTo(99));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStage_notFound_returns400() {
        StageDto request = new StageDto();
        request.setName("updated-name");
        request.setDisplayOrder(1);

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/stages/nonexistent-id-xyz")
            .then()
            .statusCode(400)
            .body("detail", containsString("not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStage_changeParent() throws Exception {
        commitData(() -> {
            Stage parent1 = new Stage();
            parent1.setName("api-stage-parent-1");
            parent1.setDisplayOrder(1);
            em.persist(parent1);

            Stage parent2 = new Stage();
            parent2.setName("api-stage-parent-2");
            parent2.setDisplayOrder(2);
            em.persist(parent2);

            Stage child = new Stage();
            child.setName("api-stage-change-parent");
            child.setDisplayOrder(3);
            child.setParentStage(parent1);
            em.persist(child);
        });

        Stage child = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-stage-change-parent")
            .getSingleResult();

        StageDto request = new StageDto();
        request.setName("api-stage-change-parent");
        request.setDisplayOrder(3);
        request.setParentStageName("api-stage-parent-2");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/stages/" + child.getId())
            .then()
            .statusCode(200)
            .body("parentStageName", equalTo("api-stage-parent-2"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStage_removeParent() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-stage-parent-remove");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("api-stage-remove-parent");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);
        });

        Stage child = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-stage-remove-parent")
            .getSingleResult();

        StageDto request = new StageDto();
        request.setName("api-stage-remove-parent");
        request.setDisplayOrder(2);
        request.setParentStageName(""); // Empty string to clear parent

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/stages/" + child.getId())
            .then()
            .statusCode(200)
            .body("parentStageName", equalTo(null));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testUpdateStage_circularInheritance_returns400() throws Exception {
        commitData(() -> {
            Stage parent = new Stage();
            parent.setName("api-stage-circular-parent");
            parent.setDisplayOrder(1);
            em.persist(parent);

            Stage child = new Stage();
            child.setName("api-stage-circular-child");
            child.setDisplayOrder(2);
            child.setParentStage(parent);
            em.persist(child);
        });

        Stage parent = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-stage-circular-parent")
            .getSingleResult();

        StageDto request = new StageDto();
        request.setName("api-stage-circular-parent");
        request.setDisplayOrder(1);
        request.setParentStageName("api-stage-circular-child");

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .put("/api/stages/" + parent.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("circular"));
    }

    // ==================== DELETE STAGE ====================

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "abstratium-abstoggle_user" })
    void testDeleteStage_removesStage() throws Exception {
        commitData(() -> {
            Stage stage = new Stage();
            stage.setName("api-stage-delete");
            stage.setDisplayOrder(1);
            em.persist(stage);
        });

        Stage existing = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-stage-delete")
            .getSingleResult();

        given()
            .when()
            .delete("/api/stages/" + existing.getId())
            .then()
            .statusCode(200);

        // Verify it's gone
        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
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

        Stage existing = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-delete-stage-with-rules")
            .getSingleResult();

        given()
            .when()
            .delete("/api/stages/" + existing.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("Remove the assignments first"));
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

        Stage parent = em.createQuery("SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", "api-parent-stage")
            .getSingleResult();

        given()
            .when()
            .delete("/api/stages/" + parent.getId())
            .then()
            .statusCode(400)
            .body("detail", containsString("child stages"));
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

    // ==================== SECURITY ====================

    @Test
    void testGetAllStages_withoutAuth_returns401or400() {
        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(org.hamcrest.Matchers.anyOf(equalTo(400), equalTo(401)));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "some_other_role" })
    void testGetAllStages_withoutProperRole_returns403() {
        given()
            .when()
            .get("/api/stages")
            .then()
            .statusCode(403);
    }
}
