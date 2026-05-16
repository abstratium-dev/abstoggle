package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ToggleStageRuleService {

    @Inject
    EntityManager em;

    @Transactional
    public List<ToggleStageRule> findAll() {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr ORDER BY tsr.toggle.name, tsr.stage.name, tsr.priority",
            ToggleStageRule.class)
            .getResultList();
    }

    @Transactional
    public Optional<ToggleStageRule> findById(String id) {
        ToggleStageRule tsr = em.find(ToggleStageRule.class, id);
        return Optional.ofNullable(tsr);
    }

    @Transactional
    public List<ToggleStageRule> findByToggleName(String toggleName) {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr WHERE tsr.toggle.name = :toggleName ORDER BY tsr.stage.name, tsr.priority",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }

    @Transactional
    public List<ToggleStageRule> findByStageName(String stageName) {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr WHERE tsr.stage.name = :stageName ORDER BY tsr.toggle.name, tsr.priority",
            ToggleStageRule.class)
            .setParameter("stageName", stageName)
            .getResultList();
    }

    @Transactional
    public Optional<ToggleStageRule> findByToggleStageAndRule(String toggleName, String stageName, String ruleName) {
        List<ToggleStageRule> results = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName AND tsr.rule.name = :ruleName",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .setParameter("ruleName", ruleName)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Creates a ToggleStageRule using entity IDs. Uses em.find() which benefits from
     * EntityManager cache - no additional SQL if entities are already loaded.
     */
    @Transactional
    public ToggleStageRule create(String toggleId, String stageId, String ruleId, String ruleValue, Integer priority) {
        Toggle toggle = em.find(Toggle.class, toggleId);
        if (toggle == null) {
            throw new IllegalArgumentException("Toggle not found: " + toggleId);
        }

        Stage stage = em.find(Stage.class, stageId);
        if (stage == null) {
            throw new IllegalArgumentException("Stage not found: " + stageId);
        }

        Rule rule = em.find(Rule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }

        // Check for duplicate assignment using names (unique constraint is on names)
        Optional<ToggleStageRule> existing = findByToggleStageAndRule(toggle.getName(), stage.getName(), rule.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Assignment already exists for toggle '" + toggle.getName() +
                "', stage '" + stage.getName() + "', and rule '" + rule.getName() + "'");
        }

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggle);
        tsr.setStage(stage);
        tsr.setRule(rule);
        tsr.setRuleValue(ruleValue != null ? ruleValue : "off");
        tsr.setPriority(priority != null ? priority : 100);

        em.persist(tsr);
        return tsr;
    }

    @Transactional
    public ToggleStageRule update(String id, String ruleValue, Integer priority) {
        ToggleStageRule tsr = em.find(ToggleStageRule.class, id);
        if (tsr == null) {
            throw new IllegalArgumentException("ToggleStageRule not found with id: " + id);
        }

        if (ruleValue != null) {
            tsr.setRuleValue(ruleValue);
        }

        if (priority != null) {
            tsr.setPriority(priority);
        }

        em.merge(tsr);
        return tsr;
    }

    @Transactional
    public void delete(String id) {
        ToggleStageRule tsr = em.find(ToggleStageRule.class, id);
        if (tsr != null) {
            em.remove(tsr);
        }
    }

    @Transactional
    public void removeTSRFromToggle(String toggleName, String stageName) {
        List<ToggleStageRule> assignments = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
        for (ToggleStageRule tsr : assignments) {
            em.remove(tsr);
        }
    }

    @Transactional
    public boolean isToggleConfiguredForStage(String toggleName, String stageName) {
        Long count = em.createQuery(
            "SELECT COUNT(tsr) FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName",
            Long.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getSingleResult();
        return count > 0;
    }

    @Transactional
    public List<String> getStagesForToggle(String toggleName) {
        return em.createQuery(
            "SELECT s.name FROM Stage s " +
            "WHERE s.id IN (SELECT tsr.stage.id FROM ToggleStageRule tsr WHERE tsr.toggle.name = :toggleName) " +
            "ORDER BY s.displayOrder, s.name",
            String.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }
}
