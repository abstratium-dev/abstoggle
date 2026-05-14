package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

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

    @Transactional
    public String addStageToToggle(String toggleName, String stageName) {
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found: " + toggleName);
        }
        Optional<Stage> stageOpt = stageService.findByName(stageName);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stageName);
        }
        return stageOpt.get().getName();
    }

    @Transactional
    public void removeStageFromToggle(String toggleName, String stageName) {
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
            "SELECT DISTINCT tsr.stage.name FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName " +
            "ORDER BY tsr.stage.displayOrder, tsr.stage.name",
            String.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }

    @Transactional
    public List<String> getTogglesForStage(String stageName) {
        return em.createQuery(
            "SELECT DISTINCT tsr.toggle.name FROM ToggleStageRule tsr " +
            "WHERE tsr.stage.name = :stageName ORDER BY tsr.toggle.name",
            String.class)
            .setParameter("stageName", stageName)
            .getResultList();
    }

    @Transactional
    public long countByToggle(String toggleName) {
        return em.createQuery(
            "SELECT COUNT(DISTINCT tsr.stage.id) FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName",
            Long.class)
            .setParameter("toggleName", toggleName)
            .getSingleResult();
    }

    @Transactional
    public long countByStage(String stageName) {
        return em.createQuery(
            "SELECT COUNT(DISTINCT tsr.toggle.id) FROM ToggleStageRule tsr " +
            "WHERE tsr.stage.name = :stageName",
            Long.class)
            .setParameter("stageName", stageName)
            .getSingleResult();
    }

    @Transactional
    public List<ToggleStageRule> findAllAssignments() {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "ORDER BY tsr.toggle.name, tsr.stage.displayOrder, tsr.stage.name, tsr.priority",
            ToggleStageRule.class)
            .getResultList();
    }
}
