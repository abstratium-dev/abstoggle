package dev.abstratium.abstoggle.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.abstratium.abstoggle.entity.Stage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StageService {

    @Inject
    EntityManager em;

    @Transactional
    public List<Stage> findAll() {
        return em.createQuery("SELECT s FROM Stage s ORDER BY s.displayOrder, s.name", Stage.class).getResultList();
    }

    @Transactional
    public Optional<Stage> findById(String id) {
        Stage stage = em.find(Stage.class, id);
        return Optional.ofNullable(stage);
    }

    @Transactional
    public Optional<Stage> findByName(String name) {
        List<Stage> results = em.createQuery(
            "SELECT s FROM Stage s WHERE s.name = :name", Stage.class)
            .setParameter("name", name)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public Stage create(String name, String description, Integer displayOrder, String parentStageName) {
        Stage stage = new Stage();
        stage.setName(name);
        stage.setDescription(description);
        stage.setDisplayOrder(displayOrder != null ? displayOrder : 0);

        if (parentStageName != null && !parentStageName.trim().isEmpty()) {
            Optional<Stage> parentStage = findByName(parentStageName);
            if (parentStage.isEmpty()) {
                throw new IllegalArgumentException("Parent stage not found: " + parentStageName);
            }
            stage.setParentStage(parentStage.get());
        }

        em.persist(stage);
        return stage;
    }

    @Transactional
    public Stage update(String id, String name, String description, Integer displayOrder, String parentStageName) {
        Stage stage = em.find(Stage.class, id);
        if (stage == null) {
            throw new IllegalArgumentException("Stage not found with id: " + id);
        }
        
        if (name != null && !name.equals(stage.getName())) {
            // Check if new name already exists
            Optional<Stage> existing = findByName(name);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Stage with name '" + name + "' already exists");
            }
            stage.setName(name);
        }
        
        if (description != null) {
            stage.setDescription(description);
        }
        
        if (displayOrder != null) {
            stage.setDisplayOrder(displayOrder);
        }
        
        if (parentStageName != null) {
            if (parentStageName.trim().isEmpty()) {
                stage.setParentStage(null);
            } else {
                Optional<Stage> parentStage = findByName(parentStageName);
                if (parentStage.isEmpty()) {
                    throw new IllegalArgumentException("Parent stage not found: " + parentStageName);
                }
                
                // Check for circular inheritance
                if (wouldCreateCircularInheritance(id, parentStage.get().getId())) {
                    throw new IllegalArgumentException("Setting this parent would create circular inheritance");
                }
                
                stage.setParentStage(parentStage.get());
            }
        }
        
        em.merge(stage);
        return stage;
    }

    @Transactional
    public void delete(String id) {
        Stage stage = em.find(Stage.class, id);
        if (stage != null) {
            // Check if any stages reference this as parent
            List<Stage> childStages = em.createQuery(
                "SELECT s FROM Stage s WHERE s.parentStage.id = :parentId", Stage.class)
                .setParameter("parentId", id)
                .getResultList();
            
            if (!childStages.isEmpty()) {
                throw new IllegalArgumentException("Cannot delete stage that has child stages: " + 
                    childStages.stream().map(Stage::getName).reduce((a, b) -> a + ", " + b).orElse(""));
            }

            // Check if any toggle-stage-rule assignments reference this stage
            List<dev.abstratium.abstoggle.entity.ToggleStageRule> assignments = em.createQuery(
                "SELECT tsr FROM ToggleStageRule tsr WHERE tsr.stage.id = :stageId",
                dev.abstratium.abstoggle.entity.ToggleStageRule.class)
                .setParameter("stageId", id)
                .getResultList();
            if (!assignments.isEmpty()) {
                throw new IllegalArgumentException("Cannot delete stage: it is still assigned to " + assignments.size() + " toggle(s). Remove the assignments first, on the toggles page.");
            }
            
            em.remove(stage);
        }
    }

    /**
     * Get the inheritance chain for a stage, starting from the stage itself
     * and walking up through parent stages.
     */
    private List<Stage> getInheritanceChain(String stageName) {
        List<Stage> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        Optional<Stage> currentOpt = findByName(stageName);
        while (currentOpt.isPresent() && !visited.contains(currentOpt.get().getName())) {
            Stage current = currentOpt.get();
            chain.add(current);
            visited.add(current.getName());
            
            current = Optional.ofNullable(current.getParentStage()).flatMap(s -> findById(s.getId())).orElse(null);
            currentOpt = Optional.ofNullable(current);
        }
        
        return chain;
    }

    /**
     * Get all stages in the inheritance chain (including the stage itself)
     * for inheritance lookup purposes. The list is ordered from the stage itself
     * to the root parent.
     */
    @Transactional
    public List<String> getInheritanceChainNames(String stageName) {
        List<Stage> chain = getInheritanceChain(stageName);
        return chain.stream().map(Stage::getName).toList();
    }

    /**
     * Check if setting parentStageId as parent of stageId would create circular inheritance.
     */
    private boolean wouldCreateCircularInheritance(String stageId, String parentStageId) {
        if (stageId.equals(parentStageId)) {
            return true;
        }
        
        Set<String> visited = new HashSet<>();
        String currentId = parentStageId;
        
        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);
            
            Stage current = em.find(Stage.class, currentId);
            if (current == null) {
                break;
            }
            
            if (currentId.equals(stageId)) {
                return true;
            }
            
            current = current.getParentStage();
            currentId = current != null ? current.getId() : null;
        }
        
        return false;
    }
}
