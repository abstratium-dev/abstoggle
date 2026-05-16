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

    @Inject
    ToggleService toggleService;

    @Inject
    StageService stageService;

    @Inject
    RuleService ruleService;

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

    @Transactional
    public ToggleStageRule create(String toggleName, String stageName, String ruleName, String ruleValue, Integer priority) {
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found: " + toggleName);
        }

        Optional<Stage> stageOpt = stageService.findByName(stageName);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stageName);
        }

        Optional<Rule> ruleOpt = ruleService.findByName(ruleName);
        if (ruleOpt.isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + ruleName);
        }

        // Check for duplicate assignment
        Optional<ToggleStageRule> existing = findByToggleStageAndRule(toggleName, stageName, ruleName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Assignment already exists for toggle '" + toggleName +
                "', stage '" + stageName + "', and rule '" + ruleName + "'");
        }

        ToggleStageRule tsr = new ToggleStageRule();
        tsr.setToggle(toggleOpt.get());
        tsr.setStage(stageOpt.get());
        tsr.setRule(ruleOpt.get());
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
