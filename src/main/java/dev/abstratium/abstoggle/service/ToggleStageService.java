package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ToggleStageService {

    @Inject
    EntityManager em;

    @Inject
    ToggleService toggleService;

    @Inject
    StageService stageService;

    @Transactional
    public Optional<ToggleStage> findById(String id) {
        ToggleStage toggleStage = em.find(ToggleStage.class, id);
        return Optional.ofNullable(toggleStage);
    }

    @Transactional
    public Optional<ToggleStage> findByToggleAndStage(String toggleName, String stageName) {
        List<ToggleStage> results = em.createQuery(
            "SELECT ts FROM ToggleStage ts WHERE ts.toggle.name = :toggleName AND ts.stage.name = :stageName", 
            ToggleStage.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public List<ToggleStage> findByToggle(String toggleName) {
        return em.createQuery(
            "SELECT ts FROM ToggleStage ts WHERE ts.toggle.name = :toggleName ORDER BY ts.stage.displayOrder, ts.stage.name", 
            ToggleStage.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }

    @Transactional
    public List<ToggleStage> findByStage(String stageName) {
        return em.createQuery(
            "SELECT ts FROM ToggleStage ts WHERE ts.stage.name = :stageName ORDER BY ts.toggle.name", 
            ToggleStage.class)
            .setParameter("stageName", stageName)
            .getResultList();
    }

    @Transactional
    public ToggleStage addStageToToggle(String toggleName, String stageName) {
        // Check if already exists
        Optional<ToggleStage> existing = findByToggleAndStage(toggleName, stageName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Toggle '" + toggleName + 
                "' is already configured for stage '" + stageName + "'");
        }
        
        // Find toggle and stage
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found: " + toggleName);
        }
        
        Optional<Stage> stageOpt = stageService.findByName(stageName);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stageName);
        }
        
        // Create toggle stage
        ToggleStage toggleStage = new ToggleStage();
        toggleStage.setToggle(toggleOpt.get());
        toggleStage.setStage(stageOpt.get());
        
        em.persist(toggleStage);
        return toggleStage;
    }

    @Transactional
    public void removeStageFromToggle(String toggleName, String stageName) {
        Optional<ToggleStage> toggleStageOpt = findByToggleAndStage(toggleName, stageName);
        if (toggleStageOpt.isPresent()) {
            // Check if there are rules associated with this toggle stage
            Long ruleCount = em.createQuery(
                "SELECT COUNT(tr) FROM ToggleRule tr WHERE tr.toggleStage.id = :toggleStageId", 
                Long.class)
                .setParameter("toggleStageId", toggleStageOpt.get().getId())
                .getSingleResult();
            
            if (ruleCount > 0) {
                throw new IllegalArgumentException("Cannot remove stage '" + stageName + 
                    "' from toggle '" + toggleName + "' because it has " + ruleCount + 
                    " associated rules. Remove the rules first.");
            }
            
            em.remove(toggleStageOpt.get());
        }
    }

    @Transactional
    public List<ToggleStage> findAll() {
        return em.createQuery(
            "SELECT ts FROM ToggleStage ts ORDER BY ts.toggle.name, ts.stage.displayOrder, ts.stage.name", 
            ToggleStage.class)
            .getResultList();
    }

    @Transactional
    public boolean isToggleConfiguredForStage(String toggleName, String stageName) {
        return findByToggleAndStage(toggleName, stageName).isPresent();
    }

    @Transactional
    public List<String> getStagesForToggle(String toggleName) {
        return em.createQuery(
            "SELECT s.name FROM ToggleStage ts JOIN ts.stage s WHERE ts.toggle.name = :toggleName ORDER BY s.displayOrder, s.name", 
            String.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }

    @Transactional
    public List<String> getTogglesForStage(String stageName) {
        return em.createQuery(
            "SELECT t.name FROM ToggleStage ts JOIN ts.toggle t WHERE ts.stage.name = :stageName ORDER BY t.name", 
            String.class)
            .setParameter("stageName", stageName)
            .getResultList();
    }

    @Transactional
    public long countByToggle(String toggleName) {
        return em.createQuery(
            "SELECT COUNT(ts) FROM ToggleStage ts WHERE ts.toggle.name = :toggleName", 
            Long.class)
            .setParameter("toggleName", toggleName)
            .getSingleResult();
    }

    @Transactional
    public long countByStage(String stageName) {
        return em.createQuery(
            "SELECT COUNT(ts) FROM ToggleStage ts WHERE ts.stage.name = :stageName", 
            Long.class)
            .setParameter("stageName", stageName)
            .getSingleResult();
    }
}
