package dev.abstratium.abstoggle.boundary.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import dev.abstratium.abstoggle.Roles;
import dev.abstratium.abstoggle.dto.ToggleStageRuleDto;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ToggleStageRuleResourceTest {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction userTransaction;

    private Toggle testToggle;
    private Stage testStage;
    private Rule testRule;
    private ToggleStageRule createdAssignment;

    @BeforeEach
    void setUp() throws Exception {
        userTransaction.begin();

        testToggle = new Toggle();
        testToggle.setName("api-tsr-test-toggle");
        testToggle.setEnabled(true);
        em.persist(testToggle);

        testStage = new Stage();
        testStage.setName("api-tsr-test-stage");
        testStage.setDisplayOrder(1);
        em.persist(testStage);

        testRule = new Rule();
        testRule.setName("api-tsr-test-rule");
        em.persist(testRule);

        em.flush();
        userTransaction.commit();
    }

    @AfterEach
    void tearDown() throws Exception {
        userTransaction.begin();
        if (createdAssignment != null && createdAssignment.getId() != null) {
            ToggleStageRule tsr = em.find(ToggleStageRule.class, createdAssignment.getId());
            if (tsr != null) {
                em.remove(tsr);
            }
        }
        // Clean up entities created in setUp
        Toggle toggle = em.find(Toggle.class, testToggle.getId());
        if (toggle != null) {
            em.remove(toggle);
        }
        Stage stage = em.find(Stage.class, testStage.getId());
        if (stage != null) {
            em.remove(stage);
        }
        Rule rule = em.find(Rule.class, testRule.getId());
        if (rule != null) {
            em.remove(rule);
        }
        userTransaction.commit();
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testCreateAssignment_success() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleId(testToggle.getId());
        request.setStageId(testStage.getId());
        request.setRuleId(testRule.getId());
        request.setToggleValue("enabled");
        request.setPriority(50);

        String id = given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Creating toggle-stage-rule assignment")
            .when()
            .post("/api/toggle-stage-rules")
            .then()
            .statusCode(200)
            .body("toggleId", equalTo(testToggle.getId()))
            .body("stageId", equalTo(testStage.getId()))
            .body("ruleId", equalTo(testRule.getId()))
            .body("toggleValue", equalTo("enabled"))
            .body("priority", equalTo(50))
            .extract().path("id");

        // Store for cleanup
        createdAssignment = new ToggleStageRule();
        createdAssignment.setId(id);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testCreateAssignment_missingToggleId_returns400() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setStageId(testStage.getId());
        request.setRuleId(testRule.getId());
        request.setToggleValue("on");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Test missing toggle ID")
            .when()
            .post("/api/toggle-stage-rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle ID is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testCreateAssignment_missingStageId_returns400() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleId(testToggle.getId());
        request.setRuleId(testRule.getId());
        request.setToggleValue("on");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Test missing stage ID")
            .when()
            .post("/api/toggle-stage-rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("Stage ID is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testCreateAssignment_missingRuleId_returns400() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleId(testToggle.getId());
        request.setStageId(testStage.getId());
        request.setToggleValue("on");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Test missing rule ID")
            .when()
            .post("/api/toggle-stage-rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("Rule ID is required"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testCreateAssignment_nonexistentToggle_returns400() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleId("nonexistent-toggle-id");
        request.setStageId(testStage.getId());
        request.setRuleId(testRule.getId());
        request.setToggleValue("on");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Test nonexistent toggle")
            .when()
            .post("/api/toggle-stage-rules")
            .then()
            .statusCode(400)
            .body("detail", containsString("Toggle not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testGetAllAssignments_returnsList() {
        given()
            .when()
            .get("/api/toggle-stage-rules")
            .then()
            .statusCode(200)
            .body("$", isA(java.util.List.class));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testGetAllAssignments_withToggleIdFilter() {
        given()
            .queryParam("toggleId", testToggle.getId())
            .when()
            .get("/api/toggle-stage-rules")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testGetAllAssignments_withStageIdFilter() {
        given()
            .queryParam("stageId", testStage.getId())
            .when()
            .get("/api/toggle-stage-rules")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testUpdateAssignment_success() throws Exception {
        // First create an assignment in a committed transaction so REST can see it
        userTransaction.begin();
        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(em.find(Toggle.class, testToggle.getId()));
        tsr.setStage(em.find(Stage.class, testStage.getId()));
        tsr.setRule(em.find(Rule.class, testRule.getId()));
        tsr.setToggleValue("original");
        tsr.setPriority(10);
        em.persist(tsr);
        em.flush();
        userTransaction.commit();

        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleValue("updated");
        request.setPriority(99);

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Updating toggle-stage-rule")
            .when()
            .put("/api/toggle-stage-rules/" + tsr.getId())
            .then()
            .statusCode(200)
            .body("id", equalTo(tsr.getId()))
            .body("toggleValue", equalTo("updated"))
            .body("priority", equalTo(99));

        // Cleanup
        userTransaction.begin();
        ToggleStageRule toRemove = em.find(ToggleStageRule.class, tsr.getId());
        if (toRemove != null) {
            em.remove(toRemove);
        }
        userTransaction.commit();
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testUpdateAssignment_nonexistent_returns400() {
        ToggleStageRuleDto request = new ToggleStageRuleDto();
        request.setToggleValue("updated");

        given()
            .contentType("application/json")
            .body(request)
            .queryParam("changeNote", "Update nonexistent assignment")
            .when()
            .put("/api/toggle-stage-rules/nonexistent-id")
            .then()
            .statusCode(400)
            .body("detail", containsString("not found"));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    void testDeleteAssignment_success() throws Exception {
        // First create an assignment in a committed transaction so REST can see it
        userTransaction.begin();
        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(em.find(Toggle.class, testToggle.getId()));
        tsr.setStage(em.find(Stage.class, testStage.getId()));
        tsr.setRule(em.find(Rule.class, testRule.getId()));
        tsr.setToggleValue("on");
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();
        userTransaction.commit();

        given()
            .queryParam("changeNote", "Deleting toggle-stage-rule")
            .when()
            .delete("/api/toggle-stage-rules/" + tsr.getId())
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { Roles.USER })
    @TestTransaction
    void testDeleteAssignment_nonexistent_returns200() {
        given()
            .queryParam("changeNote", "Delete nonexistent assignment")
            .when()
            .delete("/api/toggle-stage-rules/nonexistent-id-xyz")
            .then()
            .statusCode(200);
    }

    // ==================== SECURITY ====================

    @Test
    void testGetAllAssignments_withoutAuth_returns401or302or400() {
        given()
            .redirects().follow(false)
            .when()
            .get("/api/toggle-stage-rules")
            .then()
            .statusCode(anyOf(equalTo(302), equalTo(400), equalTo(401)));
    }

    @Test
    @TestSecurity(user = "testuser@example.com", roles = { "wrong_role" })
    @TestTransaction
    void testGetAllAssignments_withoutProperRole_returns403() {
        given()
            .when()
            .get("/api/toggle-stage-rules")
            .then()
            .statusCode(anyOf(equalTo(403), equalTo(401)));
    }
}
