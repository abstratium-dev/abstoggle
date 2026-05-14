package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class ToggleRuleServiceTest {

    @Inject
    ToggleRuleService toggleRuleService;

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void testCreateRule_generatesNameAndPersistsRule() {
        Stage stage = new Stage();
        stage.setName("rule-test-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("rule-test-toggle");
        toggle.setContext("global");
        em.persist(toggle);

        em.flush();

        ToggleRule rule = toggleRuleService.createRule(
            "rule-test-toggle",
            "rule-test-stage",
            "on",
            1,
            "Test description",
            List.of(new ToggleRuleService.CriterionData("country", "/de/i"))
        );

        assertNotNull(rule.getId(), "Rule should have an ID after persist");
        assertNotNull(rule.getName(), "Rule name should be auto-generated");
        assertFalse(rule.getName().isBlank(), "Rule name should not be blank");
        assertEquals("on", rule.getRuleValue());
        assertEquals("Test description", rule.getDescription());

        // Verify assignment was created
        List<ToggleStageRule> assignments = toggleRuleService.getAssignmentsForToggleAndStage(
            "rule-test-toggle", "rule-test-stage");
        assertEquals(1, assignments.size(), "Should have one assignment");
        assertEquals(1, assignments.get(0).getPriority());
    }

    @Test
    @TestTransaction
    void testCreateRule_withNullCriteria_createsRuleWithoutCriteria() {
        Stage stage = new Stage();
        stage.setName("rule-null-criteria-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("rule-null-criteria-toggle");
        toggle.setContext("global");
        em.persist(toggle);

        em.flush();

        ToggleRule rule = toggleRuleService.createRule(
            "rule-null-criteria-toggle",
            "rule-null-criteria-stage",
            "off",
            10,
            null,
            null
        );

        assertNotNull(rule.getName());
        assertEquals("off", rule.getRuleValue());

        List<ToggleStageRule> assignments = toggleRuleService.getAssignmentsForToggleAndStage(
            "rule-null-criteria-toggle", "rule-null-criteria-stage");
        assertEquals(1, assignments.size());
    }

    @Test
    @TestTransaction
    void testCreateRule_duplicatePriority_throwsException() {
        Stage stage = new Stage();
        stage.setName("rule-dup-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("rule-dup-toggle");
        toggle.setContext("global");
        em.persist(toggle);

        em.flush();

        toggleRuleService.createRule(
            "rule-dup-toggle", "rule-dup-stage", "on", 5, "First", null);

        assertThrows(IllegalArgumentException.class, () ->
            toggleRuleService.createRule(
                "rule-dup-toggle", "rule-dup-stage", "off", 5, "Second", null)
        );
    }
}
