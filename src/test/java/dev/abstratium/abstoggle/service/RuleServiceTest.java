package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.CriterionDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class RuleServiceTest {

    @Inject
    RuleService ruleService;

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void testCreateRule_persists_ruleWithCriteria() {
        List<CriterionDto> criteria = List.of(
            new CriterionDto(null, "country", "/de/i", null)
        );

        Rule rule = ruleService.createRule("svc-test-rule-1", "Test description", criteria);

        assertNotNull(rule.getId(), "Rule should have an ID after persist");
        assertEquals("svc-test-rule-1", rule.getName());
        assertEquals("Test description", rule.getDescription());

        List<Criterion> saved = ruleService.getCriteriaForRule(rule.getId());
        assertEquals(1, saved.size());
        assertEquals("country", saved.get(0).getCriterionKey());
        assertEquals("/de/i", saved.get(0).getCriterionValue());
    }

    @Test
    @TestTransaction
    void testCreateRule_withNullCriteria_createsRuleWithoutCriteria() {
        Rule rule = ruleService.createRule("svc-test-rule-no-criteria", null, null);

        assertNotNull(rule.getId());
        assertEquals("svc-test-rule-no-criteria", rule.getName());
        assertNull(rule.getDescription());

        List<Criterion> saved = ruleService.getCriteriaForRule(rule.getId());
        assertTrue(saved.isEmpty(), "No criteria should be persisted");
    }

    @Test
    @TestTransaction
    void testCreateRule_duplicateName_throwsException() {
        ruleService.createRule("svc-test-rule-dup", "First", null);

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.createRule("svc-test-rule-dup", "Second", null)
        );
    }

    @Test
    @TestTransaction
    void testCreateRule_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            ruleService.createRule("  ", "desc", null)
        );
    }

    @Test
    @TestTransaction
    void testFindByName_returnsRule_whenExists() {
        ruleService.createRule("svc-test-find-rule", "desc", null);

        Optional<Rule> found = ruleService.findByName("svc-test-find-rule");

        assertTrue(found.isPresent());
        assertEquals("svc-test-find-rule", found.get().getName());
    }

    @Test
    @TestTransaction
    void testFindByName_returnsEmpty_whenNotExists() {
        Optional<Rule> found = ruleService.findByName("nonexistent-rule-xyz");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindAll_includesCreatedRules() {
        ruleService.createRule("svc-findall-rule-a", null, null);
        ruleService.createRule("svc-findall-rule-b", null, null);

        List<Rule> all = ruleService.findAll();

        long count = all.stream()
            .filter(r -> r.getName().startsWith("svc-findall-rule-"))
            .count();
        assertEquals(2, count);
    }

    @Test
    @TestTransaction
    void testUpdateRule_updatesNameAndDescription() {
        Rule created = ruleService.createRule("svc-test-update-rule", "original", null);

        Rule updated = ruleService.updateRule(
            created.getId(), "svc-test-update-rule-renamed", "updated desc", null);

        assertEquals("svc-test-update-rule-renamed", updated.getName());
        assertEquals("updated desc", updated.getDescription());
    }

    @Test
    @TestTransaction
    void testUpdateRule_replacesCriteria() {
        List<CriterionDto> initial = List.of(
            new CriterionDto(null, "env", "prod", null)
        );
        Rule created = ruleService.createRule("svc-test-update-criteria-rule", "desc", initial);

        List<CriterionDto> replacement = List.of(
            new CriterionDto(null, "region", "eu-west", null),
            new CriterionDto(null, "tier", "premium", null)
        );
        ruleService.updateRule(created.getId(), null, null, replacement);

        List<Criterion> criteria = ruleService.getCriteriaForRule(created.getId());
        assertEquals(2, criteria.size());
        assertTrue(criteria.stream().anyMatch(c -> "region".equals(c.getCriterionKey())));
        assertTrue(criteria.stream().anyMatch(c -> "tier".equals(c.getCriterionKey())));
    }

    @Test
    @TestTransaction
    void testUpdateRule_notFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            ruleService.updateRule("nonexistent-id", "new-name", null, null)
        );
    }

    @Test
    @TestTransaction
    void testDeleteRule_removesRuleAndCriteria() {
        List<CriterionDto> criteria = List.of(
            new CriterionDto(null, "country", "us", null)
        );
        Rule created = ruleService.createRule("svc-test-delete-rule", "to delete", criteria);
        String id = created.getId();

        ruleService.deleteRule(id);

        assertFalse(ruleService.findByName("svc-test-delete-rule").isPresent());
        assertTrue(ruleService.getCriteriaForRule(id).isEmpty());
    }

    @Test
    @TestTransaction
    void testDeleteRule_whenAssigned_throwsException() {
        Rule rule = ruleService.createRule("svc-test-assigned-rule", "assigned", null);

        dev.abstratium.abstoggle.entity.Toggle toggle = new dev.abstratium.abstoggle.entity.Toggle();
        toggle.setName("svc-test-tsr-toggle");
        toggle.setContext("global");
        em.persist(toggle);

        dev.abstratium.abstoggle.entity.Stage stage = new dev.abstratium.abstoggle.entity.Stage();
        stage.setName("svc-test-tsr-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.deleteRule(rule.getId())
        );
    }
}
