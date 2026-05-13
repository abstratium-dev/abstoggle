package dev.abstratium.abstoggle.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleStage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ToggleRuleService {

    @Inject
    EntityManager em;

    @Inject
    StageService stageService;

    @Inject
    ToggleService toggleService;

    @Transactional
    public Optional<ToggleRule> findById(String id) {
        ToggleRule rule = em.find(ToggleRule.class, id);
        return Optional.ofNullable(rule);
    }

    @Transactional
    public List<ToggleRule> findByToggleStage(String toggleStageId) {
        return em.createQuery(
            "SELECT tr FROM ToggleRule tr WHERE tr.toggleStage.id = :toggleStageId ORDER BY tr.priority ASC", 
            ToggleRule.class)
            .setParameter("toggleStageId", toggleStageId)
            .getResultList();
    }

    @Transactional
    public List<ToggleRule> findByToggleAndStage(String toggleName, String stageName) {
        // Find toggle stage first
        Optional<ToggleStage> toggleStageOpt = findToggleStage(toggleName, stageName);
        if (toggleStageOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        return findByToggleStage(toggleStageOpt.get().getId());
    }

    @Transactional
    public ToggleRule createRule(String toggleName, String stageName, String ruleValue, 
                                Integer priority, String description, List<CriterionData> criteria) {
        // Find or create toggle stage
        ToggleStage toggleStage = findOrCreateToggleStage(toggleName, stageName);
        
        // Validate priority uniqueness within the toggle stage
        List<ToggleRule> existingRules = findByToggleStage(toggleStage.getId());
        for (ToggleRule existing : existingRules) {
            if (existing.getPriority().equals(priority)) {
                throw new IllegalArgumentException("Rule with priority " + priority + 
                    " already exists for toggle " + toggleName + " in stage " + stageName);
            }
        }
        
        ToggleRule rule = new ToggleRule();
        rule.setToggleStage(toggleStage);
        rule.setRuleValue(ruleValue != null ? ruleValue : "off");
        rule.setPriority(priority != null ? priority : 100);
        rule.setDescription(description);
        
        em.persist(rule);
        
        // Add criteria if provided
        if (criteria != null && !criteria.isEmpty()) {
            for (CriterionData criterionData : criteria) {
                ToggleCriterion criterion = new ToggleCriterion();
                criterion.setToggleRule(rule);
                criterion.setCriterionKey(criterionData.getKey());
                criterion.setCriterionValue(criterionData.getValue());
                em.persist(criterion);
            }
        }
        
        return rule;
    }

    @Transactional
    public ToggleRule updateRule(String ruleId, String ruleValue, Integer priority, 
                                String description, List<CriterionData> criteria) {
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found with id: " + ruleId);
        }
        
        // Validate priority uniqueness if changing
        if (priority != null && !priority.equals(rule.getPriority())) {
            List<ToggleRule> existingRules = findByToggleStage(rule.getToggleStage().getId());
            for (ToggleRule existing : existingRules) {
                if (!existing.getId().equals(ruleId) && existing.getPriority().equals(priority)) {
                    throw new IllegalArgumentException("Rule with priority " + priority + 
                        " already exists in this toggle stage");
                }
            }
            rule.setPriority(priority);
        }
        
        if (ruleValue != null) {
            rule.setRuleValue(ruleValue);
        }
        
        if (description != null) {
            rule.setDescription(description);
        }
        
        // Update criteria - remove existing and add new ones
        if (criteria != null) {
            // Remove existing criteria
            em.createQuery("DELETE FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId")
                .setParameter("ruleId", ruleId)
                .executeUpdate();
            
            // Add new criteria
            for (CriterionData criterionData : criteria) {
                ToggleCriterion criterion = new ToggleCriterion();
                criterion.setToggleRule(rule);
                criterion.setCriterionKey(criterionData.getKey());
                criterion.setCriterionValue(criterionData.getValue());
                em.persist(criterion);
            }
        }
        
        em.merge(rule);
        return rule;
    }

    @Transactional
    public void deleteRule(String ruleId) {
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule != null) {
            // Remove associated criteria first
            em.createQuery("DELETE FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId")
                .setParameter("ruleId", ruleId)
                .executeUpdate();
            
            em.remove(rule);
        }
    }

    @Transactional
    public void deleteRuleByPriority(String toggleName, String stageName, Integer priority) {
        List<ToggleRule> rules = findByToggleAndStage(toggleName, stageName);
        for (ToggleRule rule : rules) {
            if (rule.getPriority().equals(priority)) {
                deleteRule(rule.getId());
                break;
            }
        }
    }

    @Transactional
    public List<ToggleRule> reorderRules(String toggleName, String stageName, List<Integer> newPriorities) {
        List<ToggleRule> rules = findByToggleAndStage(toggleName, stageName);
        if (rules.size() != newPriorities.size()) {
            throw new IllegalArgumentException("Number of rules (" + rules.size() + 
                ") does not match number of new priorities (" + newPriorities.size() + ")");
        }
        
        // Validate no duplicate priorities
        for (int i = 0; i < newPriorities.size(); i++) {
            for (int j = i + 1; j < newPriorities.size(); j++) {
                if (newPriorities.get(i).equals(newPriorities.get(j))) {
                    throw new IllegalArgumentException("Duplicate priority found: " + newPriorities.get(i));
                }
            }
        }
        
        // Update priorities
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).setPriority(newPriorities.get(i));
            em.merge(rules.get(i));
        }
        
        // Return updated rules sorted by new priority
        return findByToggleAndStage(toggleName, stageName);
    }

    @Transactional
    public List<ToggleCriterion> getCriteriaForRule(String ruleId) {
        return em.createQuery(
            "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId ORDER BY tc.criterionKey", 
            ToggleCriterion.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
    }

    private Optional<ToggleStage> findToggleStage(String toggleName, String stageName) {
        List<ToggleStage> results = em.createQuery(
            "SELECT ts FROM ToggleStage ts WHERE ts.toggle.name = :toggleName AND ts.stage.name = :stageName", 
            ToggleStage.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private ToggleStage findOrCreateToggleStage(String toggleName, String stageName) {
        Optional<ToggleStage> toggleStageOpt = findToggleStage(toggleName, stageName);
        if (toggleStageOpt.isPresent()) {
            return toggleStageOpt.get();
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
        
        // Create new toggle stage
        ToggleStage toggleStage = new ToggleStage();
        toggleStage.setToggle(toggleOpt.get());
        toggleStage.setStage(stageOpt.get());
        
        em.persist(toggleStage);
        return toggleStage;
    }

    // Data transfer class for criteria
    public static class CriterionData {
        private String key;
        private String value;

        public CriterionData() {}

        public CriterionData(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
