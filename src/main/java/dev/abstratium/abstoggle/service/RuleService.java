package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.dto.CriterionDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RuleService {

    @Inject
    EntityManager em;

    @Inject
    StageService stageService;

    @Inject
    ToggleService toggleService;

    @Transactional
    public Optional<Rule> findById(String id) {
        Rule rule = em.find(Rule.class, id);
        return Optional.ofNullable(rule);
    }

    @Transactional
    public Optional<Rule> findByName(String name) {
        List<Rule> results = em.createQuery(
            "SELECT r FROM Rule r WHERE r.name = :name", Rule.class)
            .setParameter("name", name)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public List<Rule> findAll() {
        return em.createQuery("SELECT r FROM Rule r ORDER BY r.name", Rule.class)
            .getResultList();
    }

    @Transactional
    public Rule createRule(String name, String description, List<CriterionDto> criteria) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
        Optional<Rule> existing = findByName(name.trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Rule with name '" + name.trim() + "' already exists");
        }

        Rule rule = new Rule();
        rule.setName(name.trim());
        rule.setDescription(description);
        em.persist(rule);

        if (criteria != null && !criteria.isEmpty()) {
            for (CriterionDto criterionData : criteria) {
                Criterion criterion = new Criterion();
                criterion.setRule(rule);
                criterion.setCriterionKey(criterionData.getCriterionKey());
                criterion.setCriterionValue(criterionData.getCriterionValue());
                em.persist(criterion);
            }
        }

        return rule;
    }

    @Transactional
    public Rule updateRule(String ruleId, String name, String description, List<CriterionDto> criteria) {
        Rule rule = em.find(Rule.class, ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found with id: " + ruleId);
        }

        if (name != null && !name.trim().isEmpty()) {
            if (!name.trim().equals(rule.getName())) {
                Optional<Rule> existing = findByName(name.trim());
                if (existing.isPresent()) {
                    throw new IllegalArgumentException("Rule with name '" + name.trim() + "' already exists");
                }
                rule.setName(name.trim());
            }
        }
        if (description != null) {
            rule.setDescription(description);
        }

        if (criteria != null) {
            List<Criterion> existing = em.createQuery(
                "SELECT tc FROM Criterion tc WHERE tc.rule.id = :ruleId",
                Criterion.class)
                .setParameter("ruleId", ruleId)
                .getResultList();
            for (Criterion tc : existing) {
                em.remove(tc);
            }
            for (CriterionDto criterionData : criteria) {
                Criterion criterion = new Criterion();
                criterion.setRule(rule);
                criterion.setCriterionKey(criterionData.getCriterionKey());
                criterion.setCriterionValue(criterionData.getCriterionValue());
                em.persist(criterion);
            }
        }

        em.merge(rule);
        return rule;
    }

    @Transactional
    public void deleteRule(String ruleId) {
        Rule rule = em.find(Rule.class, ruleId);
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
        List<Criterion> criteria = em.createQuery(
            "SELECT tc FROM Criterion tc WHERE tc.rule.id = :ruleId",
            Criterion.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
        for (Criterion tc : criteria) {
            em.remove(tc);
        }
        em.remove(rule);
    }

    @Transactional
    public List<Criterion> getCriteriaForRule(String ruleId) {
        return em.createQuery(
            "SELECT tc FROM Criterion tc WHERE tc.rule.id = :ruleId ORDER BY tc.criterionKey",
            Criterion.class)
            .setParameter("ruleId", ruleId)
            .getResultList();
    }
}
