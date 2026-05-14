package dev.abstratium.abstoggle.service;

import java.util.List;
import java.util.Optional;

import dev.abstratium.abstoggle.entity.Toggle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ToggleService {

    @Inject
    EntityManager em;

    @Transactional
    public List<Toggle> findAll() {
        return em.createQuery("SELECT t FROM Toggle t ORDER BY t.name", Toggle.class).getResultList();
    }

    @Transactional
    public Optional<Toggle> findById(String id) {
        Toggle toggle = em.find(Toggle.class, id);
        return Optional.ofNullable(toggle);
    }

    @Transactional
    public Optional<Toggle> findByName(String name) {
        List<Toggle> results = em.createQuery(
            "SELECT t FROM Toggle t WHERE t.name = :name", Toggle.class)
            .setParameter("name", name)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public Toggle create(String name, String description, Boolean enabled, String context) {
        Toggle toggle = new Toggle();
        toggle.setName(name);
        toggle.setDescription(description);
        toggle.setEnabled(enabled != null ? enabled : true);
        toggle.setContext(context != null ? context : "");

        em.persist(toggle);
        return toggle;
    }

    @Transactional
    public Toggle update(String id, String name, String description, Boolean enabled) {
        Toggle toggle = em.find(Toggle.class, id);
        if (toggle == null) {
            throw new IllegalArgumentException("Toggle not found with id: " + id);
        }
        
        if (name != null && !name.equals(toggle.getName())) {
            // Check if new name already exists
            Optional<Toggle> existing = findByName(name);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Toggle with name '" + name + "' already exists");
            }
            toggle.setName(name);
        }
        
        if (description != null) {
            toggle.setDescription(description);
        }
        
        if (enabled != null) {
            toggle.setEnabled(enabled);
        }
        
        em.merge(toggle);
        return toggle;
    }

    @Transactional
    public Toggle updateByName(String name, String description, Boolean enabled, String context) {
        Optional<Toggle> toggleOpt = findByName(name);
        if (toggleOpt.isEmpty()) {
            throw new IllegalArgumentException("Toggle not found with name: " + name);
        }
        
        Toggle toggle = toggleOpt.get();
        if (description != null) {
            toggle.setDescription(description);
        }
        
        if (enabled != null) {
            toggle.setEnabled(enabled);
        }

        if (context != null) {
            toggle.setContext(context);
        }
        
        em.merge(toggle);
        return toggle;
    }

    @Transactional
    public void delete(String id) {
        Toggle toggle = em.find(Toggle.class, id);
        if (toggle != null) {
            List<dev.abstratium.abstoggle.entity.ToggleStageRule> assignments = em.createQuery(
                "SELECT tsr FROM ToggleStageRule tsr WHERE tsr.toggle.id = :toggleId",
                dev.abstratium.abstoggle.entity.ToggleStageRule.class)
                .setParameter("toggleId", id)
                .getResultList();
            if (!assignments.isEmpty()) {
                throw new IllegalArgumentException("Cannot delete toggle: it is still used by " + assignments.size() + " rule assignment(s). Remove the rules first.");
            }
            em.remove(toggle);
        }
    }

    @Transactional
    public void deleteByName(String name) {
        Optional<Toggle> toggleOpt = findByName(name);
        if (toggleOpt.isPresent()) {
            delete(toggleOpt.get().getId());
        }
    }

    @Transactional
    public List<Toggle> findAll(String assignedToStage, String assignedToRule) {
        boolean hasStage = assignedToStage != null && !assignedToStage.isBlank();
        boolean hasRule = assignedToRule != null && !assignedToRule.isBlank();

        if (!hasStage && !hasRule) {
            return findAll();
        }

        StringBuilder jpql = new StringBuilder("SELECT DISTINCT t FROM Toggle t");
        jpql.append(" JOIN ToggleStageRule tsr ON tsr.toggle.id = t.id");

        if (hasStage) {
            jpql.append(" JOIN tsr.stage s");
        }
        if (hasRule) {
            jpql.append(" JOIN tsr.rule r");
        }

        jpql.append(" WHERE 1=1");

        if (hasStage) {
            jpql.append(" AND s.name = :stageName");
        }
        if (hasRule) {
            jpql.append(" AND r.name = :ruleName");
        }

        jpql.append(" ORDER BY t.name");

        var query = em.createQuery(jpql.toString(), Toggle.class);
        if (hasStage) {
            query.setParameter("stageName", assignedToStage);
        }
        if (hasRule) {
            query.setParameter("ruleName", assignedToRule);
        }
        return query.getResultList();
    }

    @Transactional
    public List<Toggle> findByNameFilter(String nameFilter) {
        if (nameFilter == null || nameFilter.trim().isEmpty()) {
            return findAll();
        }
        
        return em.createQuery(
            "SELECT t FROM Toggle t WHERE t.name LIKE :namePattern ORDER BY t.name", 
            Toggle.class)
            .setParameter("namePattern", nameFilter)
            .getResultList();
    }

    @Transactional
    public List<Toggle> findByEnabled(Boolean enabled) {
        return em.createQuery(
            "SELECT t FROM Toggle t WHERE t.enabled = :enabled ORDER BY t.name", 
            Toggle.class)
            .setParameter("enabled", enabled)
            .getResultList();
    }
}
