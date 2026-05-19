package dev.abstratium.abstoggle.service;

import java.util.ArrayList;
import java.util.List;

import dev.abstratium.abstoggle.config.RevisionInfo;
import dev.abstratium.abstoggle.dto.HistoryChangeDto;
import dev.abstratium.abstoggle.dto.HistoryEntryDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Rule;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;

@ApplicationScoped
public class HistoryService {

    @Inject
    EntityManager em;

    /**
     * Searches REVINFO entries, optionally filtering by username or change note.
     * Returns results ordered by revision descending (newest first).
     *
     * @param search optional substring to filter on username or change_note (case-insensitive)
     * @param limit  max number of results (default 50)
     * @param offset pagination offset
     */
    @Transactional
    public List<HistoryEntryDto> searchHistory(String search, int limit, int offset) {
        String jpql;
        List<RevisionInfo> results;

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            jpql = "SELECT r FROM RevisionInfo r WHERE LOWER(r.username) LIKE :pattern OR LOWER(r.changeNote) LIKE :pattern ORDER BY r.rev DESC";
            results = em.createQuery(jpql, RevisionInfo.class)
                .setParameter("pattern", pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        } else {
            jpql = "SELECT r FROM RevisionInfo r ORDER BY r.rev DESC";
            results = em.createQuery(jpql, RevisionInfo.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        }

        List<HistoryEntryDto> dtos = new ArrayList<>(results.size());
        for (RevisionInfo r : results) {
            dtos.add(new HistoryEntryDto(r.getRev(), r.getRevtstmp(), r.getUsername(), r.getChangeNote(), r.getCorrelationId()));
        }
        return dtos;
    }

    /**
     * Returns the detailed list of entity changes for a given revision number.
     * Covers Toggle, Stage, Rule, Criterion and ToggleStageRule audit tables.
     */
    @Transactional
    public List<HistoryChangeDto> getRevisionDetails(long rev) {
        AuditReader reader = AuditReaderFactory.get(em);
        List<HistoryChangeDto> changes = new ArrayList<>();

        changes.addAll(collectChanges(reader, Toggle.class, "Toggle", rev));
        changes.addAll(collectChanges(reader, Stage.class, "Stage", rev));
        changes.addAll(collectChanges(reader, Rule.class, "Rule", rev));
        changes.addAll(collectChanges(reader, Criterion.class, "Criterion", rev));
        changes.addAll(collectChanges(reader, ToggleStageRule.class, "ToggleStageRule", rev));

        return changes;
    }

    @SuppressWarnings("unchecked")
    private <T> List<HistoryChangeDto> collectChanges(AuditReader reader, Class<T> entityClass, String tableName, long rev) {
        List<HistoryChangeDto> result = new ArrayList<>();

        List<Object[]> rows = reader.createQuery()
            .forEntitiesModifiedAtRevision(entityClass, rev)
            .addProjection(AuditEntity.id())
            .addProjection(AuditEntity.revisionType())
            .getResultList();

        for (Object[] row : rows) {
            String entityId = row[0] != null ? row[0].toString() : null;
            org.hibernate.envers.RevisionType revisionType = (org.hibernate.envers.RevisionType) row[1];
            int revtype = revisionType != null ? revisionType.getRepresentation() : -1;

            T entity = null;
            try {
                entity = reader.find(entityClass, entityId, rev);
            } catch (Exception e) {
                // entity may have been deleted
            }

            String data = entityToString(entity, entityId);
            result.add(new HistoryChangeDto(tableName, entityId, revtype, data));
        }

        return result;
    }

    /**
     * Returns all revisions for a specific entity identified by its table name and ID.
     * Results are ordered by revision descending (newest first).
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public List<dev.abstratium.abstoggle.dto.EntityRevisionDto> getEntityHistory(String table, String entityId) {
        Class<?> entityClass = mapTableToClass(table);
        if (entityClass == null) {
            throw new IllegalArgumentException("Unknown table: " + table);
        }

        AuditReader reader = AuditReaderFactory.get(em);
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.id().eq(entityId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        List<dev.abstratium.abstoggle.dto.EntityRevisionDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Object entity = row[0];
            RevisionInfo revInfo = (RevisionInfo) row[1];
            org.hibernate.envers.RevisionType revType = (org.hibernate.envers.RevisionType) row[2];
            int revtype = revType != null ? revType.getRepresentation() : -1;
            String data = entityToString(entity, entityId);
            result.add(new dev.abstratium.abstoggle.dto.EntityRevisionDto(
                    revInfo.getRev(), revInfo.getRevtstmp(), revInfo.getUsername(),
                    revInfo.getChangeNote(), revtype, data));
        }
        return result;
    }

    private Class<?> mapTableToClass(String table) {
        return switch (table) {
            case "Toggle" -> Toggle.class;
            case "Stage" -> Stage.class;
            case "Rule" -> Rule.class;
            case "Criterion" -> Criterion.class;
            case "ToggleStageRule" -> ToggleStageRule.class;
            default -> null;
        };
    }

    private String entityToString(Object entity, String fallbackId) {
        if (entity == null) {
            return "id=" + fallbackId;
        }
        if (entity instanceof Toggle t) {
            return "name=" + t.getName() + ", enabled=" + t.getEnabled() + ", context=" + t.getContext() + ", description=" + t.getDescription();
        }
        if (entity instanceof Stage s) {
            return "name=" + s.getName() + ", description=" + s.getDescription() + ", displayOrder=" + s.getDisplayOrder() + ", parentStageName=" + (s.getParentStage() != null ? s.getParentStage().getName() : null);
        }
        if (entity instanceof Rule r) {
            return "name=" + r.getName() + ", description=" + r.getDescription();
        }
        if (entity instanceof Criterion c) {
            return "key=" + c.getCriterionKey() + ", value=" + c.getCriterionValue();
        }
        if (entity instanceof ToggleStageRule tsr) {
            String toggleId = tsr.getToggle() != null ? tsr.getToggle().getId() : null;
            String stageId = tsr.getStage() != null ? tsr.getStage().getId() : null;
            String ruleId = tsr.getRule() != null ? tsr.getRule().getId() : null;
            return "toggleId=" + toggleId + ", stageId=" + stageId + ", ruleId=" + ruleId + ", value=" + tsr.getToggleValue() + ", priority=" + tsr.getPriority();
        }
        return entity.toString();
    }
}
