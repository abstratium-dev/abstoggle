package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.ClientContextEntry;
import dev.abstratium.abstoggle.dto.EvaluatorResponse;
import dev.abstratium.abstoggle.dto.EvaluatorResultDto;
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
        tsr.setToggleValue("on");
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
        tsr.setToggleValue("on");
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
        tsr.setToggleValue("parent-value");
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
        tsr.setToggleValue("child-value");
        em.persist(tsr);
        em.flush();

        QueryResponse response = toggleQueryService.queryTogglesWithoutCache("svc-query-closest-child", null, null, null);

        assertEquals(1, response.toggles().size());
        assertEquals("child-value", response.toggles().get(0).value());
    }

    // =========================================================================
    // Evaluator Tests
    // =========================================================================

    private List<ClientContextEntry> emptyContext() {
        return Collections.emptyList();
    }

    private List<ClientContextEntry> contextOf(String key, String value) {
        return List.of(new ClientContextEntry(key, value));
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_disabledToggle_returnsOff() {
        Stage stage = new Stage();
        stage.setName("svc-eval-disabled-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-disabled-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(false);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-disabled-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        // With debug=true, we get the debug message
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-disabled-stage", "global", null, emptyContext(), true);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("svc-eval-disabled-toggle", result.toggleName());
        assertEquals("off", result.resolvedValue());
        assertEquals("Toggle is disabled", result.debug());

        // With debug=false, debug is null
        response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-disabled-stage", "global", null, emptyContext(), false);
        assertNull(response.results().get(0).debug());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_catchAllRule_matches() {
        Stage stage = new Stage();
        stage.setName("svc-eval-catchall-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-catchall-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-catchall-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-catchall-stage", "global", null, emptyContext(), true);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("svc-eval-catchall-toggle", result.toggleName());
        assertEquals("on", result.resolvedValue());
        assertTrue(result.debug().contains("catch-all"));
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_criteriaMatch_resolvesValue() {
        Stage stage = new Stage();
        stage.setName("svc-eval-match-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-match-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-match-rule");
        em.persist(rule);

        Criterion criterion = new Criterion();
        criterion.setRule(rule);
        criterion.setCriterionKey("country");
        criterion.setCriterionValue("US");
        em.persist(criterion);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("enabled");
        em.persist(tsr);
        em.flush();

        List<ClientContextEntry> clientContext = contextOf("country", "US");
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-match-stage", "global", null, clientContext, true);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("svc-eval-match-toggle", result.toggleName());
        assertEquals("enabled", result.resolvedValue());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_criteriaNoMatch_returnsOff() {
        Stage stage = new Stage();
        stage.setName("svc-eval-nomatch-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-nomatch-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-nomatch-rule");
        em.persist(rule);

        Criterion criterion = new Criterion();
        criterion.setRule(rule);
        criterion.setCriterionKey("country");
        criterion.setCriterionValue("US");
        em.persist(criterion);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("enabled");
        em.persist(tsr);
        em.flush();

        List<ClientContextEntry> clientContext = contextOf("country", "DE");
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-nomatch-stage", "global", null, clientContext, true);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("svc-eval-nomatch-toggle", result.toggleName());
        assertEquals("off", result.resolvedValue());
        assertEquals("No matching rule - default", result.debug());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_regexPatternMatches() {
        Stage stage = new Stage();
        stage.setName("svc-eval-regex-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-regex-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-regex-rule");
        em.persist(rule);

        Criterion criterion = new Criterion();
        criterion.setRule(rule);
        criterion.setCriterionKey("country");
        criterion.setCriterionValue("/^(DE|AT|CH)$/i");
        em.persist(criterion);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        List<ClientContextEntry> clientContext = contextOf("country", "de");
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-regex-stage", "global", null, clientContext, false);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("on", result.resolvedValue());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_priorityOrdering() {
        Stage stage = new Stage();
        stage.setName("svc-eval-prio-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-prio-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule1 = new Rule();
        rule1.setName("svc-eval-prio-rule-high");
        em.persist(rule1);

        Rule rule2 = new Rule();
        rule2.setName("svc-eval-prio-rule-low");
        em.persist(rule2);

        Criterion criterion1 = new Criterion();
        criterion1.setRule(rule1);
        criterion1.setCriterionKey("plan");
        criterion1.setCriterionValue("premium");
        em.persist(criterion1);

        Criterion criterion2 = new Criterion();
        criterion2.setRule(rule2);
        criterion2.setCriterionKey("plan");
        criterion2.setCriterionValue(".*");
        em.persist(criterion2);

        ToggleStageRule tsr1 = new ToggleStageRule();
        tsr1.setToggle(toggle);
        tsr1.setStage(stage);
        tsr1.setRule(rule1);
        tsr1.setPriority(1);
        tsr1.setToggleValue("premium-value");
        em.persist(tsr1);

        ToggleStageRule tsr2 = new ToggleStageRule();
        tsr2.setToggle(toggle);
        tsr2.setStage(stage);
        tsr2.setRule(rule2);
        tsr2.setPriority(10);
        tsr2.setToggleValue("default-value");
        em.persist(tsr2);
        em.flush();

        List<ClientContextEntry> clientContext = contextOf("plan", "premium");
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-prio-stage", "global", null, clientContext, true);

        assertEquals(1, response.results().size());
        EvaluatorResultDto result = response.results().get(0);
        assertEquals("premium-value", result.resolvedValue());
        assertTrue(result.debug().contains("Priority 1"));
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_stageNotFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            toggleQueryService.evaluateToggles("nonexistent-stage-xyz", "global", null, emptyContext(), true)
        );
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_multipleCriteria_allMustMatch() {
        Stage stage = new Stage();
        stage.setName("svc-eval-multi-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-multi-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-multi-rule");
        em.persist(rule);

        Criterion criterion1 = new Criterion();
        criterion1.setRule(rule);
        criterion1.setCriterionKey("country");
        criterion1.setCriterionValue("US");
        em.persist(criterion1);

        Criterion criterion2 = new Criterion();
        criterion2.setRule(rule);
        criterion2.setCriterionKey("plan");
        criterion2.setCriterionValue("premium");
        em.persist(criterion2);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        // Both criteria match
        List<ClientContextEntry> clientContext = new ArrayList<>();
        clientContext.add(new ClientContextEntry("country", "US"));
        clientContext.add(new ClientContextEntry("plan", "premium"));
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-multi-stage", "global", null, clientContext, false);

        assertEquals(1, response.results().size());
        assertEquals("on", response.results().get(0).resolvedValue());

        // Only one criterion matches - change plan
        clientContext.clear();
        clientContext.add(new ClientContextEntry("country", "US"));
        clientContext.add(new ClientContextEntry("plan", "basic"));
        response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-multi-stage", "global", null, clientContext, false);

        assertEquals("off", response.results().get(0).resolvedValue());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_missingContextKey_defaultsToEmpty() {
        Stage stage = new Stage();
        stage.setName("svc-eval-missing-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-missing-toggle");
        toggle.setDescription("Test toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-missing-rule");
        em.persist(rule);

        Criterion criterion = new Criterion();
        criterion.setRule(rule);
        criterion.setCriterionKey("country");
        criterion.setCriterionValue("^$"); // Empty string pattern
        em.persist(criterion);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        // Missing key should default to empty string and match ^$
        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-missing-stage", "global", null, emptyContext(), false);

        assertEquals(1, response.results().size());
        assertEquals("on", response.results().get(0).resolvedValue());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_cacheHitReturnsTrue() {
        Stage stage = new Stage();
        stage.setName("svc-eval-cache-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-cache-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-cache-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        toggleQueryService.clearCache();

        List<ClientContextEntry> clientContext = contextOf("userId", "123");

        // First call - cache miss
        EvaluatorResponse first = toggleQueryService.evaluateToggles(
            "svc-eval-cache-stage", "global", null, clientContext, false);
        assertFalse(first.cacheHit());

        // Second call - cache hit
        EvaluatorResponse second = toggleQueryService.evaluateToggles(
            "svc-eval-cache-stage", "global", null, clientContext, false);
        assertTrue(second.cacheHit());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_differentContext_differentCacheKeys() {
        Stage stage = new Stage();
        stage.setName("svc-eval-different-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-different-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-different-rule");
        em.persist(rule);

        Criterion criterion = new Criterion();
        criterion.setRule(rule);
        criterion.setCriterionKey("country");
        criterion.setCriterionValue("US");
        em.persist(criterion);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        toggleQueryService.clearCache();

        // First context
        List<ClientContextEntry> context1 = contextOf("country", "US");
        EvaluatorResponse response1 = toggleQueryService.evaluateToggles(
            "svc-eval-different-stage", "global", null, context1, false);
        assertFalse(response1.cacheHit());
        assertEquals("on", response1.results().get(0).resolvedValue());

        // Same context again - cache hit
        EvaluatorResponse response2 = toggleQueryService.evaluateToggles(
            "svc-eval-different-stage", "global", null, context1, false);
        assertTrue(response2.cacheHit());

        // Different context - cache miss, different result
        List<ClientContextEntry> context2 = contextOf("country", "DE");
        EvaluatorResponse response3 = toggleQueryService.evaluateToggles(
            "svc-eval-different-stage", "global", null, context2, false);
        assertFalse(response3.cacheHit());
        assertEquals("off", response3.results().get(0).resolvedValue());
    }

    @Test
    @TestTransaction
    void testEvictFromEvaluatorCache_removesEntry() {
        Stage stage = new Stage();
        stage.setName("svc-eval-evict-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-evict-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-evict-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("on");
        em.persist(tsr);
        em.flush();

        toggleQueryService.clearCache();

        List<ClientContextEntry> clientContext = contextOf("test", "value");

        // First call - cache miss
        EvaluatorResponse first = toggleQueryService.evaluateToggles(
            "svc-eval-evict-stage", "global", null, clientContext, false);
        assertFalse(first.cacheHit());

        // Second call - cache hit
        EvaluatorResponse second = toggleQueryService.evaluateToggles(
            "svc-eval-evict-stage", "global", null, clientContext, false);
        assertTrue(second.cacheHit());

        // Evict cache
        toggleQueryService.evictFromEvaluatorCache("svc-eval-evict-stage", "global", null, clientContext);

        // Third call - cache miss again
        EvaluatorResponse third = toggleQueryService.evaluateToggles(
            "svc-eval-evict-stage", "global", null, clientContext, false);
        assertFalse(third.cacheHit());
    }

    @Test
    @TestTransaction
    void testGetEvaluatorCacheStats_returnsStats() {
        java.util.Map<String, Object> stats = toggleQueryService.getEvaluatorCacheStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("enabled"));
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_noAssignment_returnsEmpty() {
        Stage stage = new Stage();
        stage.setName("svc-eval-noassign-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-noassign-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);
        em.flush();

        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-noassign-stage", "global", null, emptyContext(), false);

        assertTrue(response.results().isEmpty());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_withContextFilter() {
        Stage stage = new Stage();
        stage.setName("svc-eval-ctxfilter-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle1 = new Toggle();
        toggle1.setName("svc-eval-ctxfilter-a");
        toggle1.setEnabled(true);
        toggle1.setContext("mobile");
        em.persist(toggle1);

        Toggle toggle2 = new Toggle();
        toggle2.setName("svc-eval-ctxfilter-b");
        toggle2.setEnabled(true);
        toggle2.setContext("web");
        em.persist(toggle2);

        Rule rule = new Rule();
        rule.setName("svc-eval-ctxfilter-rule");
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

        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-ctxfilter-stage", "mobile", null, emptyContext(), false);

        assertEquals(1, response.results().size());
        assertEquals("svc-eval-ctxfilter-a", response.results().get(0).toggleName());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_withNameFilter() {
        Stage stage = new Stage();
        stage.setName("svc-eval-filter-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Toggle toggle1 = new Toggle();
        toggle1.setName("svc-eval-filter-alpha");
        toggle1.setEnabled(true);
        toggle1.setContext("global");
        em.persist(toggle1);

        Toggle toggle2 = new Toggle();
        toggle2.setName("svc-eval-filter-beta");
        toggle2.setEnabled(true);
        toggle2.setContext("global");
        em.persist(toggle2);

        Rule rule = new Rule();
        rule.setName("svc-eval-filter-rule");
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

        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-filter-stage", "global", "%alpha%", emptyContext(), false);

        assertEquals(1, response.results().size());
        assertEquals("svc-eval-filter-alpha", response.results().get(0).toggleName());
    }

    @Test
    @TestTransaction
    void testEvaluateToggles_inheritanceFromParent() {
        Stage parent = new Stage();
        parent.setName("svc-eval-inherit-parent");
        parent.setDisplayOrder(1);
        em.persist(parent);

        Stage child = new Stage();
        child.setName("svc-eval-inherit-child");
        child.setDisplayOrder(2);
        child.setParentStage(parent);
        em.persist(child);

        Toggle toggle = new Toggle();
        toggle.setName("svc-eval-inherit-toggle");
        toggle.setEnabled(true);
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-eval-inherit-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(parent);
        tsr.setRule(rule);
        tsr.setPriority(1);
        tsr.setToggleValue("parent-value");
        em.persist(tsr);
        em.flush();

        EvaluatorResponse response = toggleQueryService.evaluateTogglesWithoutCache(
            "svc-eval-inherit-child", "global", null, emptyContext(), false);

        assertEquals(1, response.results().size());
        assertEquals("parent-value", response.results().get(0).resolvedValue());
    }
}
