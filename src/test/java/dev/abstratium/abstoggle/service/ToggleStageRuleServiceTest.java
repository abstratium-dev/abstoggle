package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
public class ToggleStageRuleServiceTest {

    @Inject
    ToggleStageRuleService toggleStageRuleService;

    @Inject
    EntityManager em;

    @BeforeEach
    @TestTransaction
    void setup() {
        // No cleanup needed - @TestTransaction rolls back after each test.
        // Use unique names per test to avoid conflicts with existing data.
    }

    @Test
    @TestTransaction
    void testGetStagesForToggle_returnsStagesOrderedByDisplayOrder() {
        // Create stages with different display orders
        Stage dev = new Stage();
        dev.setName("dev-ordered");
        dev.setDisplayOrder(1);
        em.persist(dev);

        Stage prod = new Stage();
        prod.setName("prod-ordered");
        prod.setDisplayOrder(3);
        em.persist(prod);

        Stage test = new Stage();
        test.setName("test-ordered");
        test.setDisplayOrder(2);
        em.persist(test);

        // Create a toggle
        Toggle toggle = new Toggle();
        toggle.setName("feature-ordered");
        toggle.setContext("global");
        em.persist(toggle);

        // Create a rule
        ToggleRule rule = new ToggleRule();
        rule.setName("rule-ordered");
        rule.setRuleValue("on");
        em.persist(rule);

        // Assign toggle to dev and prod (multiple rules for prod to test deduplication)
        ToggleStageRule tsrDev = new ToggleStageRule();
        tsrDev.setToggle(toggle);
        tsrDev.setStage(dev);
        tsrDev.setRule(rule);
        tsrDev.setPriority(100);
        em.persist(tsrDev);

        ToggleStageRule tsrProd1 = new ToggleStageRule();
        tsrProd1.setToggle(toggle);
        tsrProd1.setStage(prod);
        tsrProd1.setRule(rule);
        tsrProd1.setPriority(100);
        em.persist(tsrProd1);

        ToggleRule rule2 = new ToggleRule();
        rule2.setName("rule-ordered-2");
        rule2.setRuleValue("off");
        em.persist(rule2);

        ToggleStageRule tsrProd2 = new ToggleStageRule();
        tsrProd2.setToggle(toggle);
        tsrProd2.setStage(prod);
        tsrProd2.setRule(rule2);
        tsrProd2.setPriority(200);
        em.persist(tsrProd2);

        em.flush();

        List<String> stages = toggleStageRuleService.getStagesForToggle("feature-ordered");

        assertEquals(2, stages.size(), "Should return 2 distinct stages");
        assertEquals("dev-ordered", stages.get(0), "First stage should be dev-ordered (displayOrder=1)");
        assertEquals("prod-ordered", stages.get(1), "Second stage should be prod-ordered (displayOrder=3)");
    }

    @Test
    @TestTransaction
    void testGetStagesForToggle_returnsEmptyListWhenNoAssignments() {
        // Create a toggle with no stage assignments
        Toggle toggle = new Toggle();
        toggle.setName("unassigned-toggle-unique");
        toggle.setContext("global");
        em.persist(toggle);
        em.flush();

        List<String> stages = toggleStageRuleService.getStagesForToggle("unassigned-toggle-unique");

        assertTrue(stages.isEmpty(), "Should return empty list when toggle has no stage assignments");
    }

    @Test
    @TestTransaction
    void testIsToggleConfiguredForStage_returnsTrueWhenConfigured() {
        Stage stage = new Stage();
        stage.setName("prod-configured");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("feature-configured");
        toggle.setContext("global");
        em.persist(toggle);

        ToggleRule rule = new ToggleRule();
        rule.setName("rule-configured");
        rule.setRuleValue("on");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(100);
        em.persist(tsr);
        em.flush();

        assertTrue(toggleStageRuleService.isToggleConfiguredForStage("feature-configured", "prod-configured"));
    }

    @Test
    @TestTransaction
    void testIsToggleConfiguredForStage_returnsFalseWhenNotConfigured() {
        assertFalse(toggleStageRuleService.isToggleConfiguredForStage("nonexistent", "nonexistent"));
    }
}
