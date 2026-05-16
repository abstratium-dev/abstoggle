package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
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

    @Test
    @TestTransaction
    void testCreate_persistsAssignmentWithAllFields() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-rule");
        em.persist(rule);
        em.flush();

        ToggleStageRule tsr = toggleStageRuleService.create(
            "svc-tsr-toggle", "svc-tsr-stage", "svc-tsr-rule", "enabled", 10);

        assertNotNull(tsr.getId());
        assertEquals("svc-tsr-toggle", tsr.getToggle().getName());
        assertEquals("svc-tsr-stage", tsr.getStage().getName());
        assertEquals("svc-tsr-rule", tsr.getRule().getName());
        assertEquals("enabled", tsr.getRuleValue());
        assertEquals(10, tsr.getPriority());
    }

    @Test
    @TestTransaction
    void testCreate_withNullRuleValueAndPriority_usesDefaults() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-default-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-default-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-default-rule");
        em.persist(rule);
        em.flush();

        ToggleStageRule tsr = toggleStageRuleService.create(
            "svc-tsr-default-toggle", "svc-tsr-default-stage", "svc-tsr-default-rule", null, null);

        assertEquals("off", tsr.getRuleValue());
        assertEquals(100, tsr.getPriority());
    }

    @Test
    @TestTransaction
    void testCreate_toggleNotFound_throwsException() {
        Stage stage = new Stage();
        stage.setName("svc-tsr-missing-toggle-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-missing-toggle-rule");
        em.persist(rule);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            toggleStageRuleService.create("nonexistent-toggle", "svc-tsr-missing-toggle-stage",
                "svc-tsr-missing-toggle-rule", "on", 1)
        );
    }

    @Test
    @TestTransaction
    void testCreate_stageNotFound_throwsException() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-missing-stage-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-tsr-missing-stage-rule");
        em.persist(rule);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            toggleStageRuleService.create("svc-tsr-missing-stage-toggle", "nonexistent-stage",
                "svc-tsr-missing-stage-rule", "on", 1)
        );
    }

    @Test
    @TestTransaction
    void testCreate_ruleNotFound_throwsException() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-missing-rule-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-missing-rule-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            toggleStageRuleService.create("svc-tsr-missing-rule-toggle", "svc-tsr-missing-rule-stage",
                "nonexistent-rule", "on", 1)
        );
    }

    @Test
    @TestTransaction
    void testCreate_duplicateAssignment_throwsException() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-dup-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-dup-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-dup-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-dup-toggle", "svc-tsr-dup-stage", "svc-tsr-dup-rule", "on", 1);

        assertThrows(IllegalArgumentException.class, () ->
            toggleStageRuleService.create("svc-tsr-dup-toggle", "svc-tsr-dup-stage", "svc-tsr-dup-rule", "off", 2)
        );
    }

    @Test
    @TestTransaction
    void testFindById_returnsAssignment_whenExists() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-findbyid-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-findbyid-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-findbyid-rule");
        em.persist(rule);
        em.flush();

        ToggleStageRule created = toggleStageRuleService.create(
            "svc-tsr-findbyid-toggle", "svc-tsr-findbyid-stage", "svc-tsr-findbyid-rule", "on", 1);

        Optional<ToggleStageRule> found = toggleStageRuleService.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    @TestTransaction
    void testFindById_returnsEmpty_whenNotExists() {
        Optional<ToggleStageRule> found = toggleStageRuleService.findById("nonexistent-id-xyz");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindByToggleName_returnsAssignmentsForToggle() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-findbytoggle-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage1 = new Stage();
        stage1.setName("svc-tsr-findbytoggle-stage-a");
        stage1.setDisplayOrder(1);
        em.persist(stage1);

        Stage stage2 = new Stage();
        stage2.setName("svc-tsr-findbytoggle-stage-b");
        stage2.setDisplayOrder(2);
        em.persist(stage2);

        Rule rule = new Rule();
        rule.setName("svc-tsr-findbytoggle-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-findbytoggle-toggle", "svc-tsr-findbytoggle-stage-a",
            "svc-tsr-findbytoggle-rule", "on", 1);
        toggleStageRuleService.create("svc-tsr-findbytoggle-toggle", "svc-tsr-findbytoggle-stage-b",
            "svc-tsr-findbytoggle-rule", "off", 2);

        List<ToggleStageRule> results = toggleStageRuleService.findByToggleName("svc-tsr-findbytoggle-toggle");

        assertEquals(2, results.size());
    }

    @Test
    @TestTransaction
    void testFindByStageName_returnsAssignmentsForStage() {
        Toggle toggle1 = new Toggle();
        toggle1.setName("svc-tsr-findbystage-toggle-a");
        toggle1.setEnabled(true);
        em.persist(toggle1);

        Toggle toggle2 = new Toggle();
        toggle2.setName("svc-tsr-findbystage-toggle-b");
        toggle2.setEnabled(true);
        em.persist(toggle2);

        Stage stage = new Stage();
        stage.setName("svc-tsr-findbystage-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-findbystage-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-findbystage-toggle-a", "svc-tsr-findbystage-stage",
            "svc-tsr-findbystage-rule", "on", 1);
        toggleStageRuleService.create("svc-tsr-findbystage-toggle-b", "svc-tsr-findbystage-stage",
            "svc-tsr-findbystage-rule", "off", 2);

        List<ToggleStageRule> results = toggleStageRuleService.findByStageName("svc-tsr-findbystage-stage");

        assertEquals(2, results.size());
    }

    @Test
    @TestTransaction
    void testFindByToggleStageAndRule_returnsAssignment_whenExists() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-findtsr-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-findtsr-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-findtsr-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-findtsr-toggle", "svc-tsr-findtsr-stage", "svc-tsr-findtsr-rule", "on", 1);

        Optional<ToggleStageRule> found = toggleStageRuleService.findByToggleStageAndRule(
            "svc-tsr-findtsr-toggle", "svc-tsr-findtsr-stage", "svc-tsr-findtsr-rule");

        assertTrue(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindByToggleStageAndRule_returnsEmpty_whenNotExists() {
        Optional<ToggleStageRule> found = toggleStageRuleService.findByToggleStageAndRule(
            "nonexistent", "nonexistent", "nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindAll_returnsAllAssignments() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-findall-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-findall-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-findall-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-findall-toggle", "svc-tsr-findall-stage", "svc-tsr-findall-rule", "on", 1);

        List<ToggleStageRule> all = toggleStageRuleService.findAll();

        assertTrue(all.stream().anyMatch(tsr -> "svc-tsr-findall-toggle".equals(tsr.getToggle().getName())));
    }

    @Test
    @TestTransaction
    void testUpdate_updatesRuleValueAndPriority() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-update-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-update-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-update-rule");
        em.persist(rule);
        em.flush();

        ToggleStageRule created = toggleStageRuleService.create(
            "svc-tsr-update-toggle", "svc-tsr-update-stage", "svc-tsr-update-rule", "original", 50);

        ToggleStageRule updated = toggleStageRuleService.update(created.getId(), "updated", 99);

        assertEquals("updated", updated.getRuleValue());
        assertEquals(99, updated.getPriority());
    }

    @Test
    @TestTransaction
    void testUpdate_notFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            toggleStageRuleService.update("nonexistent-id", "value", 1)
        );
    }

    @Test
    @TestTransaction
    void testDelete_removesAssignment() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-delete-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-delete-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-delete-rule");
        em.persist(rule);
        em.flush();

        ToggleStageRule created = toggleStageRuleService.create(
            "svc-tsr-delete-toggle", "svc-tsr-delete-stage", "svc-tsr-delete-rule", "on", 1);
        String id = created.getId();

        toggleStageRuleService.delete(id);

        assertFalse(toggleStageRuleService.findById(id).isPresent());
    }

    @Test
    @TestTransaction
    void testDelete_nonexistent_doesNotThrow() {
        assertDoesNotThrow(() -> toggleStageRuleService.delete("nonexistent-id-xyz"));
    }

    @Test
    @TestTransaction
    void testRemoveTSRFromToggle_removesAllAssignmentsForToggleAndStage() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-remove-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-remove-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule1 = new Rule();
        rule1.setName("svc-tsr-remove-rule-1");
        em.persist(rule1);

        Rule rule2 = new Rule();
        rule2.setName("svc-tsr-remove-rule-2");
        em.persist(rule2);
        em.flush();

        toggleStageRuleService.create("svc-tsr-remove-toggle", "svc-tsr-remove-stage", "svc-tsr-remove-rule-1", "on", 1);
        toggleStageRuleService.create("svc-tsr-remove-toggle", "svc-tsr-remove-stage", "svc-tsr-remove-rule-2", "off", 2);

        toggleStageRuleService.removeTSRFromToggle("svc-tsr-remove-toggle", "svc-tsr-remove-stage");

        List<ToggleStageRule> remaining = toggleStageRuleService.findByToggleName("svc-tsr-remove-toggle");
        assertTrue(remaining.isEmpty());
    }

    @Test
    @TestTransaction
    void testIsToggleConfiguredForStage_returnsTrue_whenConfigured() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-configured-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage = new Stage();
        stage.setName("svc-tsr-configured-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-tsr-configured-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-configured-toggle", "svc-tsr-configured-stage",
            "svc-tsr-configured-rule", "on", 1);

        assertTrue(toggleStageRuleService.isToggleConfiguredForStage(
            "svc-tsr-configured-toggle", "svc-tsr-configured-stage"));
    }

    @Test
    @TestTransaction
    void testIsToggleConfiguredForStage_returnsFalse_whenNotConfigured() {
        assertFalse(toggleStageRuleService.isToggleConfiguredForStage(
            "nonexistent-toggle", "nonexistent-stage"));
    }

    @Test
    @TestTransaction
    void testGetStagesForToggle_returnsStageNames() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-tsr-stages-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Stage stage1 = new Stage();
        stage1.setName("svc-tsr-stages-stage-a");
        stage1.setDisplayOrder(2);
        em.persist(stage1);

        Stage stage2 = new Stage();
        stage2.setName("svc-tsr-stages-stage-b");
        stage2.setDisplayOrder(1);
        em.persist(stage2);

        Rule rule = new Rule();
        rule.setName("svc-tsr-stages-rule");
        em.persist(rule);
        em.flush();

        toggleStageRuleService.create("svc-tsr-stages-toggle", "svc-tsr-stages-stage-a",
            "svc-tsr-stages-rule", "on", 1);
        toggleStageRuleService.create("svc-tsr-stages-toggle", "svc-tsr-stages-stage-b",
            "svc-tsr-stages-rule", "off", 2);

        List<String> stages = toggleStageRuleService.getStagesForToggle("svc-tsr-stages-toggle");

        assertEquals(2, stages.size());
        assertTrue(stages.contains("svc-tsr-stages-stage-a"));
        assertTrue(stages.contains("svc-tsr-stages-stage-b"));
    }
}
