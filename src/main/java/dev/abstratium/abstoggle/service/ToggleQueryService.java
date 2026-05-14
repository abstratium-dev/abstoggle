package dev.abstratium.abstoggle.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import dev.abstratium.abstoggle.entity.ToggleStage;

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
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();
        }
        return toggleCache;
    }

    @Transactional
    public ToggleQueryResponse queryToggles(String stage, String nameFilter, Boolean includeDisabled) {
        // Build cache key
        String cacheKey = buildCacheKey(stage, nameFilter, includeDisabled);
        
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
        ToggleQueryResponse response = performQuery(stage, nameFilter, includeDisabled);
        
        // Cache the result
        if (cacheEnabled && response != null) {
            Cache<String, ToggleQueryResponse> cache = getCache();
            cache.put(cacheKey, response);
        }
        
        return response;
    }

    private ToggleQueryResponse performQuery(String stage, String nameFilter, Boolean includeDisabled) {
        // Validate stage exists
        Optional<Stage> stageOpt = stageService.findByName(stage);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stage);
        }
        
        // Get inheritance chain for stage fallback
        Set<String> stageChain = stageService.getInheritanceChainNames(stage);
        
        // Find toggles matching the criteria
        List<Toggle> toggles = findToggles(nameFilter, includeDisabled);
        
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

    private List<Toggle> findToggles(String nameFilter, Boolean includeDisabled) {
        String jpql = "SELECT t FROM Toggle t WHERE 1=1";
        Map<String, Object> params = new HashMap<>();
        
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
        // Try to find toggle stage in the inheritance chain
        ToggleStage toggleStage = findToggleStageInChain(toggle.getName(), stageChain);
        if (toggleStage == null) {
            // Toggle not configured for this stage chain
            return null;
        }
        
        // Get rules for this toggle stage
        List<ToggleRule> rules = em.createQuery(
            "SELECT tr FROM ToggleRule tr WHERE tr.toggleStage.id = :toggleStageId ORDER BY tr.priority ASC", 
            ToggleRule.class)
            .setParameter("toggleStageId", toggleStage.getId())
            .getResultList();
        
        // Build rule DTOs with criteria
        List<RuleDto> ruleDtos = new ArrayList<>();
        for (ToggleRule rule : rules) {
            RuleDto ruleDto = buildRuleDto(rule);
            ruleDtos.add(ruleDto);
        }
        
        // Determine which stage actually provided the configuration
        String actualStage = toggleStage.getStage().getName();
        
        return new ToggleDto(toggle.getName(), actualStage, toggle.getDescription(), toggle.getEnabled(), ruleDtos);
    }

    private ToggleStage findToggleStageInChain(String toggleName, Set<String> stageChain) {
        // Query for toggle stages in the inheritance chain, ordered by stage hierarchy
        List<ToggleStage> stages = em.createQuery(
            "SELECT ts FROM ToggleStage ts WHERE ts.toggle.name = :toggleName AND ts.stage.name IN :stageNames ORDER BY ts.stage.displayOrder ASC", 
            ToggleStage.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageNames", stageChain)
            .getResultList();
        
        return stages.isEmpty() ? null : stages.get(0);
    }

    private RuleDto buildRuleDto(ToggleRule rule) {
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
            rule.getPriority(),
            rule.getRuleValue(),
            rule.getDescription(),
            criteriaMap
        );
    }

    private String buildCacheKey(String stage, String nameFilter, Boolean includeDisabled) {
        StringBuilder key = new StringBuilder();
        key.append("stage:").append(stage != null ? stage : "");
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
