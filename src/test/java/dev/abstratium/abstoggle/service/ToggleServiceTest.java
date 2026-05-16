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
public class ToggleServiceTest {

    @Inject
    ToggleService toggleService;

    @Inject
    EntityManager em;

    @Test
    @TestTransaction
    void testCreate_persistsToggleWithAllFields() {
        Toggle toggle = toggleService.create("svc-toggle-create", "A description", false, "my-context");

        assertNotNull(toggle.getId());
        assertEquals("svc-toggle-create", toggle.getName());
        assertEquals("A description", toggle.getDescription());
        assertFalse(toggle.getEnabled());
        assertEquals("my-context", toggle.getContext());
    }

    @Test
    @TestTransaction
    void testCreate_withNullEnabledAndContext_usesDefaults() {
        Toggle toggle = toggleService.create("svc-toggle-defaults", null, null, null);

        assertTrue(toggle.getEnabled());
        assertEquals("", toggle.getContext());
    }

    @Test
    @TestTransaction
    void testFindByName_returnsToggle_whenExists() {
        toggleService.create("svc-toggle-findbyname", null, null, null);

        Optional<Toggle> found = toggleService.findByName("svc-toggle-findbyname");

        assertTrue(found.isPresent());
        assertEquals("svc-toggle-findbyname", found.get().getName());
    }

    @Test
    @TestTransaction
    void testFindByName_returnsEmpty_whenNotExists() {
        Optional<Toggle> found = toggleService.findByName("nonexistent-toggle-xyz");

        assertFalse(found.isPresent());
    }

    @Test
    @TestTransaction
    void testFindAll_noFilters_includesCreatedToggles() {
        toggleService.create("svc-findall-toggle-a", null, null, null);
        toggleService.create("svc-findall-toggle-b", null, null, null);

        List<Toggle> all = toggleService.findAll();

        long count = all.stream()
            .filter(t -> t.getName().startsWith("svc-findall-toggle-"))
            .count();
        assertEquals(2, count);
    }

    @Test
    @TestTransaction
    void testFindAll_noFilters_orderedByName() {
        toggleService.create("svc-order-toggle-z", null, null, null);
        toggleService.create("svc-order-toggle-a", null, null, null);

        List<Toggle> all = toggleService.findAll();

        List<Toggle> filtered = all.stream()
            .filter(t -> t.getName().startsWith("svc-order-toggle-"))
            .toList();
        assertEquals(2, filtered.size());
        assertEquals("svc-order-toggle-a", filtered.get(0).getName());
        assertEquals("svc-order-toggle-z", filtered.get(1).getName());
    }

    @Test
    @TestTransaction
    void testUpdate_updatesNameDescriptionEnabledAndContext() {
        Toggle created = toggleService.create("svc-toggle-update-orig", "original", true, "ctx-a");

        Toggle updated = toggleService.update(
            created.getId(), "svc-toggle-update-renamed", "updated desc", false, "ctx-b");

        assertEquals("svc-toggle-update-renamed", updated.getName());
        assertEquals("updated desc", updated.getDescription());
        assertFalse(updated.getEnabled());
        assertEquals("ctx-b", updated.getContext());
    }

    @Test
    @TestTransaction
    void testUpdate_notFound_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            toggleService.update("nonexistent-id", "new-name", null, null, null)
        );
    }

    @Test
    @TestTransaction
    void testUpdate_duplicateName_throwsException() {
        toggleService.create("svc-toggle-dup-a", null, null, null);
        Toggle b = toggleService.create("svc-toggle-dup-b", null, null, null);

        assertThrows(IllegalArgumentException.class, () ->
            toggleService.update(b.getId(), "svc-toggle-dup-a", null, null, null)
        );
    }

    @Test
    @TestTransaction
    void testUpdate_sameNameDoesNotThrow() {
        Toggle created = toggleService.create("svc-toggle-same-name", null, null, null);

        Toggle updated = toggleService.update(created.getId(), "svc-toggle-same-name", "new desc", null, null);

        assertEquals("svc-toggle-same-name", updated.getName());
        assertEquals("new desc", updated.getDescription());
    }

    @Test
    @TestTransaction
    void testDelete_removesToggle() {
        Toggle created = toggleService.create("svc-toggle-delete", null, null, null);
        String id = created.getId();
        em.flush();

        toggleService.delete(id);

        assertFalse(toggleService.findByName("svc-toggle-delete").isPresent());
    }

    @Test
    @TestTransaction
    void testDelete_nonexistent_doesNotThrow() {
        assertDoesNotThrow(() -> toggleService.delete("nonexistent-id-xyz"));
    }

    @Test
    @TestTransaction
    void testDelete_whenAssigned_throwsException() {
        Toggle toggle = toggleService.create("svc-toggle-del-assigned", null, null, null);

        Stage stage = new Stage();
        stage.setName("svc-toggle-del-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-toggle-del-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        assertThrows(IllegalArgumentException.class, () ->
            toggleService.delete(toggle.getId())
        );
    }

    @Test
    @TestTransaction
    void testFindAll_filteredByStage_returnsAssignedToggles() {
        Toggle toggle = toggleService.create("svc-filter-stage-toggle", null, null, null);

        Stage stage = new Stage();
        stage.setName("svc-filter-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-filter-stage-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        List<Toggle> result = toggleService.findAll("svc-filter-stage", null);

        assertEquals(1, result.size());
        assertEquals("svc-filter-stage-toggle", result.get(0).getName());
    }

    @Test
    @TestTransaction
    void testFindAll_filteredByRule_returnsAssignedToggles() {
        Toggle toggle = toggleService.create("svc-filter-rule-toggle", null, null, null);

        Stage stage = new Stage();
        stage.setName("svc-filter-rule-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule = new Rule();
        rule.setName("svc-filter-rule");
        em.persist(rule);

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setPriority(1);
        em.persist(tsr);
        em.flush();

        List<Toggle> result = toggleService.findAll(null, "svc-filter-rule");

        assertEquals(1, result.size());
        assertEquals("svc-filter-rule-toggle", result.get(0).getName());
    }

    @Test
    @TestTransaction
    void testFindAll_filteredByStageAndRule_returnsOnlyMatchingToggle() {
        Toggle toggle1 = toggleService.create("svc-filter-both-toggle-1", null, null, null);
        Toggle toggle2 = toggleService.create("svc-filter-both-toggle-2", null, null, null);

        Stage stage = new Stage();
        stage.setName("svc-filter-both-stage");
        stage.setDisplayOrder(1);
        em.persist(stage);

        Rule rule1 = new Rule();
        rule1.setName("svc-filter-both-rule-1");
        em.persist(rule1);

        Rule rule2 = new Rule();
        rule2.setName("svc-filter-both-rule-2");
        em.persist(rule2);

        ToggleStageRule tsr1 = new ToggleStageRule();
        tsr1.setToggle(toggle1);
        tsr1.setStage(stage);
        tsr1.setRule(rule1);
        tsr1.setPriority(1);
        em.persist(tsr1);

        ToggleStageRule tsr2 = new ToggleStageRule();
        tsr2.setToggle(toggle2);
        tsr2.setStage(stage);
        tsr2.setRule(rule2);
        tsr2.setPriority(2);
        em.persist(tsr2);
        em.flush();

        List<Toggle> result = toggleService.findAll("svc-filter-both-stage", "svc-filter-both-rule-1");

        assertEquals(1, result.size());
        assertEquals("svc-filter-both-toggle-1", result.get(0).getName());
    }

    @Test
    @TestTransaction
    void testFindAll_filteredByStage_withNonexistentStage_returnsEmpty() {
        List<Toggle> result = toggleService.findAll("nonexistent-stage-xyz", null);

        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindAll_nullFilters_delegatesToUnfilteredFindAll() {
        toggleService.create("svc-null-filter-toggle", null, null, null);

        List<Toggle> result = toggleService.findAll(null, null);

        assertTrue(result.stream().anyMatch(t -> "svc-null-filter-toggle".equals(t.getName())));
    }
}
