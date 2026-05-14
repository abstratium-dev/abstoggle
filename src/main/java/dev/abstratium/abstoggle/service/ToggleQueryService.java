package dev.abstratium.abstoggle.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import dev.abstratium.abstoggle.dto.QueryMetadata;
import dev.abstratium.abstoggle.dto.RuleDto;
import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.dto.ToggleQueryResponse;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleCriterion;
import dev.abstratium.abstoggle.entity.ToggleRule;
import dev.abstratium.abstoggle.entity.ToggleStageRule;

@ApplicationScoped
public class ToggleQueryService {

    @Inject
    EntityManager em;

    @Inject
    StageService stageService;

    @Inject
    ToggleService toggleService;

    @ConfigProperty(name = "toggle.cache.enabled", defaultValue = "true")
    boolean cacheEnabled;

    @ConfigProperty(name = "toggle.cache.ttl-seconds", defaultValue = "60")
    int cacheTtlSeconds;

    @ConfigProperty(name = "toggle.cache.max-size-mb", defaultValue = "5")
    int cacheMaxSizeMb;

    private Cache<String, ToggleQueryResponse> toggleCache;

    // Initialize cache on first use
    private Cache<String, ToggleQueryResponse> getCache() {
        if (toggleCache == null && cacheEnabled) {
            toggleCache = CacheBuilder.newBuilder()
                .maximumWeight(cacheMaxSizeMb * 1024 * 1024L)
                .weigher((String key, ToggleQueryResponse value) -> {
                    // Rough estimate of size in bytes
                    return key.length() * 2 + value.toString().length() * 2;
                })
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .recordStats()
                .build();
        }
        return toggleCache;
    }

    /**
     * Query toggles with caching enabled (for public endpoints).
     * Results are cached based on stage, nameFilter, and includeDisabled parameters.
     */
    @Transactional
    public ToggleQueryResponse queryToggles(String stage, String context, String nameFilter, Boolean includeDisabled) {
        // Build cache key
        String cacheKey = buildCacheKey(stage, context, nameFilter, includeDisabled);

        // Check cache first
        if (cacheEnabled) {
            Cache<String, ToggleQueryResponse> cache = getCache();
            ToggleQueryResponse cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                // Update cache hit flag in metadata
                cached.getQueryMetadata().setCacheHit(true);
                return cached;
            }
        }

        // Perform actual query
        ToggleQueryResponse response = performQuery(stage, context, nameFilter, includeDisabled);

        // Cache the result
        if (cacheEnabled && response != null) {
            Cache<String, ToggleQueryResponse> cache = getCache();
            cache.put(cacheKey, response);
        }

        return response;
    }

    /**
     * Query toggles without caching (for management endpoints).
     * Always fetches fresh data from the database.
     */
    @Transactional
    public ToggleQueryResponse queryTogglesWithoutCache(String stage, String context, String nameFilter, Boolean includeDisabled) {
        ToggleQueryResponse response = performQuery(stage, context, nameFilter, includeDisabled);
        // Ensure cacheHit is always false for non-cached queries
        response.getQueryMetadata().setCacheHit(false);
        return response;
    }

    private ToggleQueryResponse performQuery(String stage, String context, String nameFilter, Boolean includeDisabled) {
        // Validate stage exists
        Optional<Stage> stageOpt = stageService.findByName(stage);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stage);
        }
        
        // Get inheritance chain for stage fallback
        Set<String> stageChain = stageService.getInheritanceChainNames(stage);
        
        // Find toggles matching the criteria
        List<Toggle> toggles = findToggles(context, nameFilter, includeDisabled);
        
        // Build toggle DTOs with rules and criteria
        List<ToggleDto> toggleDtos = new ArrayList<>();
        
        for (Toggle toggle : toggles) {
            ToggleDto toggleDto = buildToggleDto(toggle, stage, stageChain);
            if (toggleDto != null) {
                toggleDtos.add(toggleDto);
            }
        }
        
        // Build metadata
        QueryMetadata metadata = new QueryMetadata(
            stage,
            nameFilter,
            toggleDtos.size(),
            false // cache hit
        );
        
        return new ToggleQueryResponse(toggleDtos, metadata);
    }

    private List<Toggle> findToggles(String context, String nameFilter, Boolean includeDisabled) {
        String jpql = "SELECT t FROM Toggle t WHERE 1=1";
        Map<String, Object> params = new HashMap<>();

        if (context != null && !context.trim().isEmpty()) {
            jpql += " AND t.context = :context";
            params.put("context", context.trim());
        }
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            // Validate regex pattern
            try {
                Pattern.compile(nameFilter);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex pattern in nameFilter: " + nameFilter);
            }
            
            jpql += " AND t.name LIKE :nameFilter";
            params.put("nameFilter", nameFilter);
        }
        
        if (includeDisabled == null || !includeDisabled) {
            jpql += " AND t.enabled = true";
        }
        
        jpql += " ORDER BY t.name";
        
        jakarta.persistence.TypedQuery<Toggle> query = em.createQuery(jpql, Toggle.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        return query.getResultList();
    }

    private ToggleDto buildToggleDto(Toggle toggle, String stage, Set<String> stageChain) {
        // Find the first matching ToggleStageRule assignment in the inheritance chain
        List<ToggleStageRule> assignments = findAssignmentsInChain(toggle.getName(), stageChain);
        if (assignments.isEmpty()) {
            // Toggle not configured for this stage chain
            return null;
        }

        // Build rule DTOs with criteria
        List<RuleDto> ruleDtos = new ArrayList<>();
        for (ToggleStageRule tsr : assignments) {
            RuleDto ruleDto = buildRuleDto(tsr);
            ruleDtos.add(ruleDto);
        }

        // Determine which stage actually provided the configuration
        String actualStage = assignments.get(0).getStage().getName();

        return new ToggleDto(toggle.getName(), actualStage, toggle.getDescription(), toggle.getEnabled(), toggle.getContext(), ruleDtos);
    }

    private List<ToggleStageRule> findAssignmentsInChain(String toggleName, Set<String> stageChain) {
        // Get all assignments for this toggle in the chain, ordered by stage displayOrder then priority
        List<ToggleStageRule> all = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name IN :stageNames " +
            "ORDER BY tsr.stage.displayOrder ASC, tsr.priority ASC",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageNames", stageChain)
            .getResultList();

        if (all.isEmpty()) {
            return all;
        }

        // Only return assignments from the closest stage in the chain
        String closestStage = all.get(0).getStage().getName();
        return all.stream()
            .filter(tsr -> tsr.getStage().getName().equals(closestStage))
            .toList();
    }

    private RuleDto buildRuleDto(ToggleStageRule assignment) {
        ToggleRule rule = assignment.getRule();
        // Get criteria for this rule
        List<ToggleCriterion> criteria = em.createQuery(
            "SELECT tc FROM ToggleCriterion tc WHERE tc.toggleRule.id = :ruleId ORDER BY tc.criterionKey",
            ToggleCriterion.class)
            .setParameter("ruleId", rule.getId())
            .getResultList();

        // Build criteria map
        Map<String, String> criteriaMap = new HashMap<>();
        for (ToggleCriterion criterion : criteria) {
            criteriaMap.put(criterion.getCriterionKey(), criterion.getCriterionValue());
        }

        return new RuleDto(
            rule.getId(),
            assignment.getPriority(),
            rule.getRuleValue(),
            rule.getDescription(),
            criteriaMap
        );
    }

    private String buildCacheKey(String stage, String context, String nameFilter, Boolean includeDisabled) {
        StringBuilder key = new StringBuilder();
        key.append("stage:").append(stage != null ? stage : "");
        key.append(":context:").append(context != null ? context : "");
        key.append(":filter:").append(nameFilter != null ? nameFilter : "");
        key.append(":includeDisabled:").append(includeDisabled != null ? includeDisabled : false);
        return key.toString();
    }

    /**
     * Clear the cache - useful for testing or admin operations
     */
    public void clearCache() {
        if (cacheEnabled && toggleCache != null) {
            toggleCache.invalidateAll();
        }
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        if (!cacheEnabled || toggleCache == null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("enabled", false);
            return stats;
        }
        
        com.google.common.cache.CacheStats stats = toggleCache.stats();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", true);
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", stats.hitRate());
        result.put("requestCount", stats.requestCount());
        result.put("size", toggleCache.size());
        result.put("maxSize", cacheMaxSizeMb * 1024 * 1024L);
        result.put("ttlSeconds", cacheTtlSeconds);
        
        return result;
    }

    /**
     * Validate that a regex pattern is syntactically correct
     */
    public boolean isValidRegex(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return true;
        }
        
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Get all available stages for validation
     */
    @Transactional
    public List<String> getAvailableStages() {
        return em.createQuery("SELECT s.name FROM Stage s ORDER BY s.displayOrder, s.name", String.class)
            .getResultList();
    }

    /**
     * Check if a toggle exists and is enabled
     */
    @Transactional
    public boolean isToggleEnabled(String toggleName) {
        Optional<Toggle> toggle = toggleService.findByName(toggleName);
        return toggle.isPresent() && toggle.get().getEnabled();
    }

    /**
     * Get toggle configuration for a specific stage (no inheritance)
     */
    @Transactional
    public ToggleDto getToggleForStage(String toggleName, String stage) {
        Optional<Toggle> toggleOpt = toggleService.findByName(toggleName);
        if (toggleOpt.isEmpty() || !toggleOpt.get().getEnabled()) {
            return null;
        }
        
        Set<String> stageChain = Set.of(stage); // Only this stage, no inheritance
        return buildToggleDto(toggleOpt.get(), stage, stageChain);
    }
}
