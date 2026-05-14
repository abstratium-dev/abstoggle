package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import io.quarkus.runtime.annotations.RegisterForReflection;
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
    public Optional<ToggleRule> findByName(String name) {
        List<ToggleRule> results = em.createQuery(
            "SELECT r FROM ToggleRule r WHERE r.name = :name", ToggleRule.class)
            .setParameter("name", name)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public List<ToggleRule> findAll() {
        return em.createQuery("SELECT r FROM ToggleRule r ORDER BY r.name", ToggleRule.class)
            .getResultList();
    }

    @Transactional
    public Optional<ToggleStageRule> findAssignment(String toggleName, String stageName, String ruleId) {
        List<ToggleStageRule> results = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName AND tsr.rule.id = :ruleId",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .setParameter("ruleId", ruleId)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public ToggleRule createRule(String toggleName, String stageName, String ruleValue,
                                Integer priority, String description, List<CriterionData> criteria) {
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found: " + toggleName);
        }
        Optional<Stage> stageOpt = stageService.findByName(stageName);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stageName);
        }

        // Validate priority uniqueness within this toggle+stage
        int resolvedPriority = priority != null ? priority : 100;
        List<ToggleStageRule> existingAssignments = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
        for (ToggleStageRule existing : existingAssignments) {
            if (existing.getPriority().equals(resolvedPriority)) {
                throw new IllegalArgumentException("A rule with priority " + resolvedPriority +
                    " already exists for toggle '" + toggleName + "' in stage '" + stageName + "'");
            }
        }

        ToggleRule rule = new ToggleRule();
        rule.setName(java.util.UUID.randomUUID().toString());
        rule.setRuleValue(ruleValue != null ? ruleValue : "off");
        rule.setDescription(description);
        em.persist(rule);

        if (criteria != null && !criteria.isEmpty()) {
            for (CriterionData criterionData : criteria) {
                ToggleCriterion criterion = new ToggleCriterion();
                criterion.setToggleRule(rule);
                criterion.setCriterionKey(criterionData.getKey());
                criterion.setCriterionValue(criterionData.getValue());
                em.persist(criterion);
            }
        }

        ToggleStageRule assignment = new ToggleStageRule();
        assignment.setToggle(toggleOpt.get());
        assignment.setStage(stageOpt.get());
        assignment.setRule(rule);
        assignment.setPriority(resolvedPriority);
        em.persist(assignment);

        return rule;
    }

    @Transactional
    public ToggleStageRule assignRule(String toggleName, String stageName, String ruleId, Integer priority) {
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found: " + toggleName);
        }
        Optional<Stage> stageOpt = stageService.findByName(stageName);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stageName);
        }
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found with id: " + ruleId);
        }

        int resolvedPriority = priority != null ? priority : 100;
        List<ToggleStageRule> existingAssignments = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
        for (ToggleStageRule existing : existingAssignments) {
            if (existing.getPriority().equals(resolvedPriority)) {
                throw new IllegalArgumentException("A rule with priority " + resolvedPriority +
                    " already exists for toggle '" + toggleName + "' in stage '" + stageName + "'");
            }
        }

        ToggleStageRule assignment = new ToggleStageRule();
        assignment.setToggle(toggleOpt.get());
        assignment.setStage(stageOpt.get());
        assignment.setRule(rule);
        assignment.setPriority(resolvedPriority);
        em.persist(assignment);
        return assignment;
    }

    @Transactional
    public ToggleRule createStandaloneRule(String name, String ruleValue, String description, List<CriterionData> criteria) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
        Optional<ToggleRule> existing = findByName(name.trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Rule with name '" + name.trim() + "' already exists");
        }

        ToggleRule rule = new ToggleRule();
        rule.setName(name.trim());
        rule.setRuleValue(ruleValue != null ? ruleValue : "off");
        rule.setDescription(description);
        em.persist(rule);

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
    public ToggleRule updateStandaloneRule(String ruleId, String name, String ruleValue, String description, List<CriterionData> criteria) {
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found with id: " + ruleId);
        }

        if (name != null && !name.trim().isEmpty()) {
            if (!name.trim().equals(rule.getName())) {
                Optional<ToggleRule> existing = findByName(name.trim());
                if (existing.isPresent()) {
                    throw new IllegalArgumentException("Rule with name '" + name.trim() + "' already exists");
                }
                rule.setName(name.trim());
            }
        }
        if (ruleValue != null) {
            rule.setRuleValue(ruleValue);
        }
        if (description != null) {
            rule.setDescription(description);
        }

        if (criteria != null) {
            List<ToggleCriterion> existing = em.createQuery(
                "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId",
                ToggleCriterion.class)
                .setParameter("ruleId", ruleId)
                .getResultList();
            for (ToggleCriterion tc : existing) {
                em.remove(tc);
            }
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
    public ToggleRule updateRule(String ruleId, String ruleValue, Integer priority,
                                String description, List<CriterionData> criteria) {
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found with id: " + ruleId);
        }

        if (ruleValue != null) {
            rule.setRuleValue(ruleValue);
        }
        if (description != null) {
            rule.setDescription(description);
        }

        if (criteria != null) {
            List<ToggleCriterion> existing = em.createQuery(
                "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId",
                ToggleCriterion.class)
                .setParameter("ruleId", ruleId)
                .getResultList();
            for (ToggleCriterion tc : existing) {
                em.remove(tc);
            }
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
    public ToggleStageRule updateAssignmentPriority(String toggleName, String stageName, String ruleId, Integer priority) {
        Optional<ToggleStageRule> assignmentOpt = findAssignment(toggleName, stageName, ruleId);
        if (assignmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Assignment not found for toggle '" + toggleName +
                "', stage '" + stageName + "', rule '" + ruleId + "'");
        }
        ToggleStageRule assignment = assignmentOpt.get();
        if (priority != null) {
            assignment.setPriority(priority);
            em.merge(assignment);
        }
        return assignment;
    }

    @Transactional
    public void unassignRule(String toggleName, String stageName, String ruleId) {
        Optional<ToggleStageRule> assignmentOpt = findAssignment(toggleName, stageName, ruleId);
        assignmentOpt.ifPresent(em::remove);
    }

    @Transactional
    public void deleteRule(String ruleId) {
        ToggleRule rule = em.find(ToggleRule.class, ruleId);
        if (rule == null) {
            return;
        }
        List<ToggleStageRule> assignments = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr WHERE tsr.rule.id = :ruleId",
            ToggleStageRule.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
        if (!assignments.isEmpty()) {
            throw new IllegalArgumentException("Rule '" + ruleId + "' is still assigned to " +
                assignments.size() + " toggle+stage combination(s). Unassign it first.");
        }
        List<ToggleCriterion> criteria = em.createQuery(
            "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId",
            ToggleCriterion.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
        for (ToggleCriterion tc : criteria) {
            em.remove(tc);
        }
        em.remove(rule);
    }

    @Transactional
    public List<ToggleCriterion> getCriteriaForRule(String ruleId) {
        return em.createQuery(
            "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId ORDER BY tc.criterionKey",
            ToggleCriterion.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
    }

    @Transactional
    public List<ToggleStageRule> getAssignmentsForToggleAndStage(String toggleName, String stageName) {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name = :stageName " +
            "ORDER BY tsr.priority ASC",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageName", stageName)
            .getResultList();
    }

    @Transactional
    public List<ToggleStageRule> getAssignmentsForToggle(String toggleName) {
        return em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "JOIN FETCH tsr.toggle " +
            "JOIN FETCH tsr.stage " +
            "JOIN FETCH tsr.rule " +
            "WHERE tsr.toggle.name = :toggleName " +
            "ORDER BY tsr.stage.displayOrder, tsr.stage.name, tsr.priority ASC",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .getResultList();
    }

    @Transactional
    public Optional<ToggleStageRule> findAssignmentById(String id) {
        List<ToggleStageRule> results = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "JOIN FETCH tsr.toggle " +
            "JOIN FETCH tsr.stage " +
            "JOIN FETCH tsr.rule " +
            "WHERE tsr.id = :id",
            ToggleStageRule.class)
            .setParameter("id", id)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public ToggleStageRule updateAssignmentPriorityById(String id, Integer priority) {
        ToggleStageRule assignment = em.find(ToggleStageRule.class, id);
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment not found with id: " + id);
        }
        if (priority != null) {
            assignment.setPriority(priority);
            em.merge(assignment);
        }
        // Reload with eager fetch to avoid lazy loading issues in the boundary layer
        List<ToggleStageRule> results = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "JOIN FETCH tsr.toggle " +
            "JOIN FETCH tsr.stage " +
            "JOIN FETCH tsr.rule " +
            "WHERE tsr.id = :id",
            ToggleStageRule.class)
            .setParameter("id", id)
            .getResultList();
        return results.isEmpty() ? assignment : results.get(0);
    }

    @Transactional
    public void unassignById(String id) {
        ToggleStageRule assignment = em.find(ToggleStageRule.class, id);
        if (assignment != null) {
            em.remove(assignment);
        }
    }

    // Data transfer class for criteria
    @RegisterForReflection
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
