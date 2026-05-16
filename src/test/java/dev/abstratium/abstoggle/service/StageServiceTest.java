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
public class StageServiceTest {

    @Inject
    StageService stageService;

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void testCreate_persistsStageWithAllFields() {
        Stage stage = stageService.create("svc-stage-create", "A description", 5, null);

        assertNotNull(stage.getId());
        assertEquals("svc-stage-create", stage.getName());
        assertEquals("A description", stage.getDescription());
        assertEquals(5, stage.getDisplayOrder());
        assertNull(stage.getParentStage());
    }

    @Test
    @TestTransaction
    void testCreate_withNullDisplayOrder_defaultsToZero() {
        Stage stage = stageService.create("svc-stage-default-order", null, null, null);

        assertEquals(0, stage.getDisplayOrder());
    }

    @Test
    @TestTransaction
    void testCreate_withParent_setsParentStage() {
        stageService.create("svc-stage-parent", null, 1, null);

        Stage child = stageService.create("svc-stage-child", null, 2, "svc-stage-parent");

        assertNotNull(child.getParentStage());
        assertEquals("svc-stage-parent", child.getParentStage().getName());
    }

    @Test
    @TestTransaction
    void testCreate_withMissingParent_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            stageService.create("svc-stage-orphan", null, 1, "nonexistent-parent")
        );
    }

    @Test
    @TestTransaction
    void testFindByName_returnsStage_whenExists() {
        stageService.create("svc-stage-findbyname", null, 1, null);

        Optional<Stage> found = stageService.findByName("svc-stage-findbyname");

        assertTrue(found.isPresent());
        assertEquals("svc-stage-findbyname", found.get().getName());
    }

    @Test
    @TestTransaction
    void testFindByName_returnsEmpty_whenNotExists() {
        Optional<Stage> found = stageService.findByName("nonexistent-stage-xyz");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindById_returnsStage_whenExists() {
        Stage created = stageService.create("svc-stage-findbyid", null, 1, null);

        Optional<Stage> found = stageService.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    @TestTransaction
    void testFindById_returnsEmpty_whenNotExists() {
        Optional<Stage> found = stageService.findById("nonexistent-id-xyz");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindAll_includesCreatedStages() {
        stageService.create("svc-findall-stage-a", null, 10, null);
        stageService.create("svc-findall-stage-b", null, 20, null);

        List<Stage> all = stageService.findAll();

        long count = all.stream()
            .filter(s -> s.getName().startsWith("svc-findall-stage-"))
            .count();
        assertEquals(2, count);
    }

    @Test
    @TestTransaction
    void testFindAll_orderedByDisplayOrder() {
        stageService.create("svc-order-stage-z", null, 99, null);
        stageService.create("svc-order-stage-a", null, 1, null);

        List<Stage> all = stageService.findAll();

        List<Stage> filtered = all.stream()
            .filter(s -> s.getName().startsWith("svc-order-stage-"))
            .toList();
        assertEquals(2, filtered.size());
        assertEquals("svc-order-stage-a", filtered.get(0).getName());
        assertEquals("svc-order-stage-z", filtered.get(1).getName());
    }

    @Test
    @TestTransaction
    void testUpdate_updatesNameDescriptionAndDisplayOrder() {
        Stage created = stageService.create("svc-stage-update-orig", "original", 1, null);

        Stage updated = stageService.update(
            created.getId(), "svc-stage-update-renamed", "updated desc", 99, null);

        assertEquals("svc-stage-update-renamed", updated.getName());
        assertEquals("updated desc", updated.getDescription());
        assertEquals(99, updated.getDisplayOrder());
    }

    @Test
    @TestTransaction
    void testUpdate_notFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            stageService.update("nonexistent-id", "new-name", null, null, null)
        );
    }

    @Test
    @TestTransaction
    void testUpdate_duplicateName_throwsException() {
        stageService.create("svc-stage-dup-a", null, 1, null);
        Stage b = stageService.create("svc-stage-dup-b", null, 2, null);

        assertThrows(IllegalArgumentException.class, () ->
            stageService.update(b.getId(), "svc-stage-dup-a", null, null, null)
        );
    }

    @Test
    @TestTransaction
    void testUpdate_setsParent() {
        stageService.create("svc-stage-new-parent", null, 1, null);
        Stage child = stageService.create("svc-stage-new-child", null, 2, null);

        Stage updated = stageService.update(child.getId(), null, null, null, "svc-stage-new-parent");

        assertNotNull(updated.getParentStage());
        assertEquals("svc-stage-new-parent", updated.getParentStage().getName());
    }

    @Test
    @TestTransaction
    void testUpdate_clearsParent_withBlankParentName() {
        stageService.create("svc-stage-clear-parent", null, 1, null);
        Stage child = stageService.create("svc-stage-clear-child", null, 2, "svc-stage-clear-parent");
        em.flush();

        Stage updated = stageService.update(child.getId(), null, null, null, "");

        assertNull(updated.getParentStage());
    }

    @Test
    @TestTransaction
    void testUpdate_circularInheritance_throwsException() {
        Stage parent = stageService.create("svc-stage-circ-parent", null, 1, null);
        stageService.create("svc-stage-circ-child", null, 2, "svc-stage-circ-parent");
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            stageService.update(parent.getId(), null, null, null, "svc-stage-circ-child")
        );
    }

    @Test
    @TestTransaction
    void testUpdate_selfAsParent_throwsException() {
        Stage stage = stageService.create("svc-stage-self-parent", null, 1, null);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            stageService.update(stage.getId(), null, null, null, "svc-stage-self-parent")
        );
    }

    @Test
    @TestTransaction
    void testDelete_removesStage() {
        Stage created = stageService.create("svc-stage-delete", null, 1, null);
        String id = created.getId();
        em.flush();

        stageService.delete(id);

        assertFalse(stageService.findById(id).isPresent());
    }

    @Test
    @TestTransaction
    void testDelete_nonexistent_doesNotThrow() {
        assertDoesNotThrow(() -> stageService.delete("nonexistent-id-xyz"));
    }

    @Test
    @TestTransaction
    void testDelete_withChildStages_throwsException() {
        stageService.create("svc-stage-del-parent", null, 1, null);
        stageService.create("svc-stage-del-child", null, 2, "svc-stage-del-parent");
        em.flush();

        Optional<Stage> parent = stageService.findByName("svc-stage-del-parent");

        assertThrows(IllegalArgumentException.class, () ->
            stageService.delete(parent.get().getId())
        );
    }

    @Test
    @TestTransaction
    void testDelete_withToggleStageRuleAssignment_throwsException() {
        Stage stage = stageService.create("svc-stage-del-assigned", null, 1, null);

        Toggle toggle = new Toggle();
        toggle.setName("svc-stage-del-toggle");
        toggle.setContext("global");
        em.persist(toggle);

        Rule rule = new Rule();
        rule.setName("svc-stage-del-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            stageService.delete(stage.getId())
        );
    }

    @Test
    @TestTransaction
    void testGetInheritanceChainNames_returnsChainFromLeafToRoot() {
        stageService.create("svc-chain-root", null, 1, null);
        stageService.create("svc-chain-mid", null, 2, "svc-chain-root");
        stageService.create("svc-chain-leaf", null, 3, "svc-chain-mid");
        em.flush();

        List<String> chain = stageService.getInheritanceChainNames("svc-chain-leaf");

        assertEquals(List.of("svc-chain-leaf", "svc-chain-mid", "svc-chain-root"), chain);
    }

    @Test
    @TestTransaction
    void testGetInheritanceChainNames_singleStage_returnsSelf() {
        stageService.create("svc-chain-solo", null, 1, null);
        em.flush();

        List<String> chain = stageService.getInheritanceChainNames("svc-chain-solo");

        assertEquals(List.of("svc-chain-solo"), chain);
    }

    @Test
    @TestTransaction
    void testGetInheritanceChainNames_nonexistent_returnsEmpty() {
        List<String> chain = stageService.getInheritanceChainNames("nonexistent-stage-xyz");

        assertTrue(chain.isEmpty());
    }
}
