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
    public Toggle create(String name, String description, String createdBy) {
        Toggle toggle = new Toggle();
        toggle.setName(name);
        toggle.setDescription(description);
        toggle.setCreatedBy(createdBy);
        toggle.setEnabled(true);
        
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
    public Toggle updateByName(String name, String description, Boolean enabled) {
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
        
        em.merge(toggle);
        return toggle;
    }

    @Transactional
    public void delete(String id) {
        Toggle toggle = em.find(Toggle.class, id);
        if (toggle != null) {
            em.remove(toggle);
        }
    }

    @Transactional
    public void deleteByName(String name) {
        Optional<Toggle> toggleOpt = findByName(name);
        if (toggleOpt.isPresent()) {
            em.remove(toggleOpt.get());
        }
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
