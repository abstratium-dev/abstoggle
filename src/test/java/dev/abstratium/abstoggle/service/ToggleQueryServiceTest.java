package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.dto.ToggleQueryResponse;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleRule;
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
    void testQueryToggles_inheritanceChainReturnsClosestStageFirst() {
        // Set up inheritance chain: leaf -> middle -> root
        // Using displayOrder values that would cause the bug if ordering by displayOrder
        Stage root = new Stage();
        root.setName("root-stage");
        root.setDisplayOrder(1);
        em.persist(root);

        Stage middle = new Stage();
        middle.setName("middle-stage");
        middle.setDisplayOrder(2);
        middle.setParentStage(root);
        em.persist(middle);

        Stage leaf = new Stage();
        leaf.setName("leaf-stage");
        leaf.setDisplayOrder(3);
        leaf.setParentStage(middle);
        em.persist(leaf);

        Toggle toggle = new Toggle();
        toggle.setName("inheritance-toggle");
        toggle.setContext("global");
        toggle.setEnabled(true);
        em.persist(toggle);

        ToggleRule ruleOn = new ToggleRule();
        ruleOn.setName("always-on");
        ruleOn.setRuleValue("on");
        em.persist(ruleOn);

        ToggleRule ruleOff = new ToggleRule();
        ruleOff.setName("always-off");
        ruleOff.setRuleValue("off");
        em.persist(ruleOff);

        // Assign root (on) and leaf (off)
        ToggleStageRule tsrRoot = new ToggleStageRule();
        tsrRoot.setToggle(toggle);
        tsrRoot.setStage(root);
        tsrRoot.setRule(ruleOn);
        tsrRoot.setPriority(100);
        em.persist(tsrRoot);

        ToggleStageRule tsrLeaf = new ToggleStageRule();
        tsrLeaf.setToggle(toggle);
        tsrLeaf.setStage(leaf);
        tsrLeaf.setRule(ruleOff);
        tsrLeaf.setPriority(100);
        em.persist(tsrLeaf);

        em.flush();
        toggleQueryService.clearCache();

        // Query for leaf stage - should return leaf's rule (off), not root's (on)
        ToggleQueryResponse response = toggleQueryService.queryToggles("leaf-stage", "global", null, false);
        assertNotNull(response);
        assertEquals(1, response.getToggles().size(), "Should return exactly one toggle");

        ToggleDto toggleDto = response.getToggles().get(0);
        assertEquals("leaf-stage", toggleDto.getStage(), "Should return the closest stage (leaf), not root");
        assertEquals(1, toggleDto.getRules().size());
        assertEquals("off", toggleDto.getRules().get(0).getValue(), "Should return leaf's 'off' rule");
    }

    @Test
    @TestTransaction
    void testQueryToggles_fallsBackToParentWhenNoAssignmentForStage() {
        Stage root = new Stage();
        root.setName("fallback-root");
        root.setDisplayOrder(1);
        em.persist(root);

        Stage child = new Stage();
        child.setName("fallback-child");
        child.setDisplayOrder(2);
        child.setParentStage(root);
        em.persist(child);

        Toggle toggle = new Toggle();
        toggle.setName("fallback-toggle");
        toggle.setContext("global");
        toggle.setEnabled(true);
        em.persist(toggle);

        ToggleRule rule = new ToggleRule();
        rule.setName("fallback-rule");
        rule.setRuleValue("on");
        em.persist(rule);

        // Only assign to root
        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(root);
        tsr.setRule(rule);
        tsr.setPriority(100);
        em.persist(tsr);

        em.flush();
        toggleQueryService.clearCache();

        // Query for child stage - should fall back to root's rule
        ToggleQueryResponse response = toggleQueryService.queryToggles("fallback-child", "global", null, false);
        assertNotNull(response);
        assertEquals(1, response.getToggles().size());

        ToggleDto toggleDto = response.getToggles().get(0);
        assertEquals("fallback-root", toggleDto.getStage(), "Should fall back to root stage");
        assertEquals("on", toggleDto.getRules().get(0).getValue());
    }

    @Test
    @TestTransaction
    void testQueryToggles_noAssignmentInChain_returnsEmpty() {
        Stage orphan = new Stage();
        orphan.setName("orphan-stage");
        orphan.setDisplayOrder(1);
        em.persist(orphan);

        Toggle toggle = new Toggle();
        toggle.setName("orphan-toggle");
        toggle.setContext("global");
        toggle.setEnabled(true);
        em.persist(toggle);

        em.flush();
        toggleQueryService.clearCache();

        ToggleQueryResponse response = toggleQueryService.queryToggles("orphan-stage", "global", null, false);
        assertNotNull(response);
        assertEquals(0, response.getToggles().size(), "Should return no toggles when no assignment in chain");
    }
}
