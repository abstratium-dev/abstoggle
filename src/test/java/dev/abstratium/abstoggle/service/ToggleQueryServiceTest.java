package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.dto.QueryTSRDto;
import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class ToggleQueryServiceTest {

    @Inject
    ToggleQueryService toggleQueryService;

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void testQueryToggles_stageNotFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            toggleQueryService.queryToggles("nonexistent-stage-xyz", null, null, null)
        );
    }

    @Test
    @TestTransaction
    void testQueryToggles_withoutCache_returnsResults() {
        Stage stage = new Stage();
        stage.setName("svc-query-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setRuleValue("on");
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-stage", null, null, null);

        assertEquals(1, response.toggles().size());
        assertEquals("svc-query-toggle", response.toggles().get(0).toggleName());
        assertEquals("on", response.toggles().get(0).value());
        assertFalse(response.queryMetadata().getCacheHit());
    }

    @Test
    @TestTransaction
    void testQueryToggles_withCriteria_returnsCriteriaInResponse() {
        Stage stage = new Stage();
        stage.setName("svc-query-criteria-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-criteria-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-criteria-rule");
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
        tsr.setRuleValue("on");
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-criteria-stage", null, null, null);

        assertEquals(1, response.toggles().size());
        QueryTSRDto dto = response.toggles().get(0);
        assertEquals(1, dto.ruleCriteria().size());
        assertEquals("country", dto.ruleCriteria().get(0).getCriterionKey());
        assertEquals("/US/i", dto.ruleCriteria().get(0).getCriterionValue());
    }

    @Test
    @TestTransaction
    void testQueryToggles_withContextFilter() {
        Stage stage = new Stage();
        stage.setName("svc-query-ctx-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle1 = new Toggle();
        toggle1.setName("svc-query-ctx-toggle-a");
        toggle1.setEnabled(true);
        toggle1.setContext("mobile");
        em.persist(toggle1);

        Toggle toggle2 = new Toggle();
        toggle2.setName("svc-query-ctx-toggle-b");
        toggle2.setEnabled(true);
        toggle2.setContext("web");
        em.persist(toggle2);

        Rule rule = new Rule();
        rule.setName("svc-query-ctx-rule");
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
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-ctx-stage", "mobile", null, null);

        assertEquals(1, response.toggles().size());
        assertEquals("svc-query-ctx-toggle-a", response.toggles().get(0).toggleName());
    }

    @Test
    @TestTransaction
    void testQueryToggles_withNameFilter() {
        Stage stage = new Stage();
        stage.setName("svc-query-filter-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-filtered-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-filter-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-filter-stage", null, "%filtered%", null);

        assertEquals(1, response.toggles().size());
    }

    @Test
    @TestTransaction
    void testQueryToggles_withInvalidRegex_throwsException() {
        Stage stage = new Stage();
        stage.setName("svc-query-regex-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            toggleQueryService.queryTogglesWithoutCache("svc-query-regex-stage", null, "[invalid", null)
        );
    }

    @Test
    @TestTransaction
    void testQueryToggles_disabledTogglesExcludedByDefault() {
        Stage stage = new Stage();
        stage.setName("svc-query-disabled-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle enabledToggle = new Toggle();
        enabledToggle.setName("svc-query-enabled-toggle");
        enabledToggle.setEnabled(true);
        em.persist(enabledToggle);

        Toggle disabledToggle = new Toggle();
        disabledToggle.setName("svc-query-disabled-toggle");
        disabledToggle.setEnabled(false);
        em.persist(disabledToggle);

        Rule rule = new Rule();
        rule.setName("svc-query-disabled-rule");
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
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-disabled-stage", null, null, null);

        long count = response.toggles().stream()
            .filter(t -> t.toggleName().startsWith("svc-query-"))
            .count();
        assertEquals(1, count);
        assertEquals("svc-query-enabled-toggle", response.toggles().get(0).toggleName());
    }

    @Test
    @TestTransaction
    void testQueryToggles_includeDisabled_returnsAll() {
        Stage stage = new Stage();
        stage.setName("svc-query-incl-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle enabledToggle = new Toggle();
        enabledToggle.setName("svc-query-incl-enabled");
        enabledToggle.setEnabled(true);
        em.persist(enabledToggle);

        Toggle disabledToggle = new Toggle();
        disabledToggle.setName("svc-query-incl-disabled");
        disabledToggle.setEnabled(false);
        em.persist(disabledToggle);

        Rule rule = new Rule();
        rule.setName("svc-query-incl-rule");
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
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-incl-stage", null, null, true);

        long count = response.toggles().stream()
            .filter(t -> t.toggleName().startsWith("svc-query-incl-"))
            .count();
        assertEquals(2, count);
    }

    @Test
    @TestTransaction
    void testQueryToggles_inheritanceChain_parentStageFallback() {
        Stage parent = new Stage();
        parent.setName("svc-query-inherit-parent");
        parent.setDisplayOrder(1);
        em.persist(parent);

        Stage child = new Stage();
        child.setName("svc-query-inherit-child");
        child.setDisplayOrder(2);
        child.setParentStage(parent);
        em.persist(child);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-inherit-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-inherit-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(parent);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setRuleValue("parent-value");
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-inherit-child", null, null, null);

        assertEquals(1, response.toggles().size());
        assertEquals("parent-value", response.toggles().get(0).value());
    }

    @Test
    @TestTransaction
    void testQueryToggles_noAssignment_returnsEmpty() {
        Stage stage = new Stage();
        stage.setName("svc-query-noassign-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-noassign-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-noassign-stage", null, null, null);

        assertTrue(response.toggles().isEmpty());
    }

    @Test
    @TestTransaction
    void testIsValidRegex_validPattern_returnsTrue() {
        assertTrue(toggleQueryService.isValidRegex("^[a-z]+$"));
    }

    @Test
    @TestTransaction
    void testIsValidRegex_invalidPattern_returnsFalse() {
        assertFalse(toggleQueryService.isValidRegex("[invalid"));
    }

    @Test
    @TestTransaction
    void testIsValidRegex_nullOrEmpty_returnsTrue() {
        assertTrue(toggleQueryService.isValidRegex(null));
        assertTrue(toggleQueryService.isValidRegex(""));
        assertTrue(toggleQueryService.isValidRegex("   "));
    }

    @Test
    @TestTransaction
    void testGetAvailableStages_returnsStageNames() {
        Stage stage1 = new Stage();
        stage1.setName("svc-query-avail-a");
        stage1.setDisplayOrder(2);
        em.persist(stage1);

        Stage stage2 = new Stage();
        stage2.setName("svc-query-avail-b");
        stage2.setDisplayOrder(1);
        em.persist(stage2);
        em.flush();

        List<String> stages = toggleQueryService.getAvailableStages();

        assertTrue(stages.contains("svc-query-avail-a"));
        assertTrue(stages.contains("svc-query-avail-b"));
    }

    @Test
    @TestTransaction
    void testIsToggleEnabled_enabledToggle_returnsTrue() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-query-is-enabled");
        toggle.setEnabled(true);
        em.persist(toggle);
        em.flush();

        assertTrue(toggleQueryService.isToggleEnabled("svc-query-is-enabled"));
    }

    @Test
    @TestTransaction
    void testIsToggleEnabled_disabledToggle_returnsFalse() {
        Toggle toggle = new Toggle();
        toggle.setName("svc-query-is-disabled");
        toggle.setEnabled(false);
        em.persist(toggle);
        em.flush();

        assertFalse(toggleQueryService.isToggleEnabled("svc-query-is-disabled"));
    }

    @Test
    @TestTransaction
    void testIsToggleEnabled_nonexistentToggle_returnsFalse() {
        assertFalse(toggleQueryService.isToggleEnabled("nonexistent-toggle-xyz"));
    }

    @Test
    @TestTransaction
    void testGetToggleForStage_returnsToggleDto() {
        Stage stage = new Stage();
        stage.setName("svc-query-get-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-get-toggle");
        toggle.setDescription("desc");
        toggle.setEnabled(true);
        toggle.setContext("ctx");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-get-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        ToggleDto dto = toggleQueryService.getToggleForStage("svc-query-get-toggle", "svc-query-get-stage");

        assertNotNull(dto);
        assertEquals("svc-query-get-toggle", dto.getName());
        assertEquals("desc", dto.getDescription());
        assertTrue(dto.getEnabled());
        assertEquals("ctx", dto.getContext());
    }

    @Test
    @TestTransaction
    void testGetToggleForStage_disabledToggle_returnsNull() {
        Stage stage = new Stage();
        stage.setName("svc-query-null-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-null-toggle");
        toggle.setEnabled(false);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-null-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        ToggleDto dto = toggleQueryService.getToggleForStage("svc-query-null-toggle", "svc-query-null-stage");

        assertNull(dto);
    }

    @Test
    @TestTransaction
    void testGetToggleForStage_noAssignment_returnsNull() {
        Stage stage = new Stage();
        stage.setName("svc-query-noassign2-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-noassign2-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);
        em.flush();

        ToggleDto dto = toggleQueryService.getToggleForStage("svc-query-noassign2-toggle", "svc-query-noassign2-stage");

        assertNull(dto);
    }

    @Test
    @TestTransaction
    void testClearCache_doesNotThrow() {
        assertDoesNotThrow(() -> toggleQueryService.clearCache());
    }

    @Test
    @TestTransaction
    void testGetCacheStats_returnsMapWithEnabled() {
        Map<String, Object> stats = toggleQueryService.getCacheStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("enabled"));
    }

    @Test
    @TestTransaction
    void testQueryToggles_cacheHitReturnsTrue() {
        Stage stage = new Stage();
        stage.setName("svc-query-cache-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-cache-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-cache-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        toggleQueryService.clearCache();

        QueryResponse first = toggleQueryService.queryToggles("svc-query-cache-stage", null, null, null);
        assertFalse(first.queryMetadata().getCacheHit());

        QueryResponse second = toggleQueryService.queryToggles("svc-query-cache-stage", null, null, null);
        assertTrue(second.queryMetadata().getCacheHit());
    }

    @Test
    @TestTransaction
    void testQueryToggles_multipleRulesSortedByPriority() {
        Stage stage = new Stage();
        stage.setName("svc-query-prio-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-prio-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule1 = new Rule();
        rule1.setName("svc-query-prio-rule-low");
        em.persist(rule1);

        Rule rule2 = new Rule();
        rule2.setName("svc-query-prio-rule-high");
        em.persist(rule2);

        ToggleStageRule tsr1 = new ToggleStageRule();
        tsr1.setToggle(toggle);
        tsr1.setStage(stage);
        tsr1.setRule(rule1);
        tsr1.setPriority(100);
        em.persist(tsr1);

        ToggleStageRule tsr2 = new ToggleStageRule();
        tsr2.setToggle(toggle);
        tsr2.setStage(stage);
        tsr2.setRule(rule2);
        tsr2.setPriority(10);
        em.persist(tsr2);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-prio-stage", null, null, null);

        assertEquals(2, response.toggles().size());
        assertEquals("svc-query-prio-rule-high", response.toggles().get(0).ruleName());
        assertEquals(10, response.toggles().get(0).priority());
        assertEquals("svc-query-prio-rule-low", response.toggles().get(1).ruleName());
        assertEquals(100, response.toggles().get(1).priority());
    }

    @Test
    @TestTransaction
    void testQueryToggles_closestStagePriority() {
        Stage parent = new Stage();
        parent.setName("svc-query-closest-parent");
        parent.setDisplayOrder(1);
        em.persist(parent);

        Stage child = new Stage();
        child.setName("svc-query-closest-child");
        child.setDisplayOrder(2);
        child.setParentStage(parent);
        em.persist(child);

        Toggle toggle = new Toggle();
        toggle.setName("svc-query-closest-toggle");
        toggle.setEnabled(true);
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-query-closest-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(child);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setRuleValue("child-value");
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-closest-child", null, null, null);

        assertEquals(1, response.toggles().size());
        assertEquals("child-value", response.toggles().get(0).value());
    }
}
