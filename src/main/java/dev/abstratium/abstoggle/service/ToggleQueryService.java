package dev.abstratium.abstoggle.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import dev.abstratium.abstoggle.dto.ClientContextEntry;
import dev.abstratium.abstoggle.dto.CriterionDto;
import dev.abstratium.abstoggle.dto.EvaluatorResponse;
import dev.abstratium.abstoggle.dto.EvaluatorResultDto;
import dev.abstratium.abstoggle.dto.QueryMetadata;
import dev.abstratium.abstoggle.dto.QueryResponse;
import dev.abstratium.abstoggle.dto.QueryTSRDto;
import dev.abstratium.abstoggle.dto.ToggleDto;
import dev.abstratium.abstoggle.entity.Criterion;
import dev.abstratium.abstoggle.entity.Stage;
import dev.abstratium.abstoggle.entity.Toggle;
import dev.abstratium.abstoggle.entity.ToggleStageRule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

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

    private Cache<String, QueryResponse> toggleCache;
    private Cache<String, EvaluatorResponse> evaluatorCache;

    @PostConstruct
    private void initCache() {
        if (cacheEnabled) {
            toggleCache = CacheBuilder.newBuilder()
                .maximumWeight(cacheMaxSizeMb * 1024 * 1024L)
                .weigher((String key, QueryResponse value) -> {
                    // Rough estimate of size in bytes
                    return key.length() * 2 + value.toString().length() * 2;
                })
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .recordStats()
                .build();

            evaluatorCache = CacheBuilder.newBuilder()
                .maximumWeight(cacheMaxSizeMb * 1024 * 1024L)
                .weigher((String key, EvaluatorResponse value) -> {
                    // Rough estimate of size in bytes
                    return key.length() * 2 + value.toString().length() * 2;
                })
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .recordStats()
                .build();
        }
    }

    /**
     * Query toggles with caching enabled (for public endpoints).
     * Results are cached based on stage, nameFilter, and includeDisabled parameters.
     */
    @Transactional
    public QueryResponse queryToggles(String stage, String context, String nameFilter, Boolean includeDisabled) {
        // Build cache key
        String cacheKey = buildCacheKey(stage, context, nameFilter, includeDisabled);

        // Check cache first
        if (cacheEnabled) {
            Cache<String, QueryResponse> cache = toggleCache;
            QueryResponse cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                // Update cache hit flag in metadata
                cached.queryMetadata().setCacheHit(true);
                return cached;
            }
        }

        // Perform actual query
        QueryResponse response = performQuery(stage, context, nameFilter, includeDisabled);

        // Cache the result
        if (cacheEnabled && response != null) {
            toggleCache.put(cacheKey, response);
        }

        return response;
    }

    /**
     * Query toggles without caching (for management endpoints).
     * Always fetches fresh data from the database.
     */
    @Transactional
    public QueryResponse queryTogglesWithoutCache(String stage, String context, String nameFilter, Boolean includeDisabled) {
        QueryResponse response = performQuery(stage, context, nameFilter, includeDisabled);
        // Ensure cacheHit is always false for non-cached queries
        response.queryMetadata().setCacheHit(false);
        return response;
    }

    private QueryResponse performQuery(String stage, String context, String nameFilter, Boolean includeDisabled) {
        // Validate stage exists
        Optional<Stage> stageOpt = stageService.findByName(stage);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stage);
        }
        
        // Get inheritance chain for stage fallback
        List<String> stageChain = stageService.getInheritanceChainNames(stage);
        
        // Find toggles matching the criteria
        List<Toggle> toggles = findToggles(context, nameFilter, includeDisabled);
        
        // Build TSR DTOs with rules and criteria
        List<QueryTSRDto> allTsrDtos = new ArrayList<>();
        
        for (Toggle toggle : toggles) {
            List<QueryTSRDto> tsrDtos = buildQueryResults(toggle, stage, stageChain);
            if (tsrDtos != null && !tsrDtos.isEmpty()) {
                allTsrDtos.addAll(tsrDtos);
            }
        }
        
        // Build metadata
        QueryMetadata metadata = new QueryMetadata(
            stage,
            nameFilter,
            allTsrDtos.size(),
            false, // cache hit
            cacheEnabled,
            cacheTtlSeconds
        );
        
        return new QueryResponse(allTsrDtos, metadata);
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
        
        TypedQuery<Toggle> query = em.createQuery(jpql, Toggle.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        return query.getResultList();
    }

    private List<QueryTSRDto> buildQueryResults(Toggle toggle, String stage, List<String> stageChain) {
        // Find the first matching ToggleStageRule assignment in the inheritance chain
        List<ToggleStageRule> assignments = findAssignmentsInChain(toggle.getName(), stageChain);
        if (assignments.isEmpty()) {
            // Toggle not configured for this stage chain
            return null;
        }

        // Build rule DTOs with criteria
        List<QueryTSRDto> tsrDtos = new ArrayList<>();
        for (ToggleStageRule tsr : assignments) {

            List<Criterion> criteria = em.createQuery("SELECT c FROM Criterion c WHERE c.rule.id = :ruleId", Criterion.class)
                .setParameter("ruleId", tsr.getRule().getId())
                .getResultList();

            List<CriterionDto> criteriaDtos = criteria.stream()
                .map(c -> new CriterionDto(c.getId(), c.getCriterionKey(), c.getCriterionValue(), c.getRule().getId()))
                .toList();

            QueryTSRDto dto = new QueryTSRDto(
                toggle.getName(),
                toggle.getDescription(),
                toggle.getEnabled(),
                toggle.getContext(),
                tsr.getStage().getName(),
                tsr.getRule().getName(),
                tsr.getRule().getDescription(),
                criteriaDtos,
                tsr.getPriority(),
                tsr.getRuleValue()
            );
            tsrDtos.add(dto);
        }
        return tsrDtos;
    }

    private List<ToggleStageRule> findAssignmentsInChain(String toggleName, List<String> stageChain) {
        // Get all assignments for this toggle in the chain
        List<ToggleStageRule> all = em.createQuery(
            "SELECT tsr FROM ToggleStageRule tsr " +
            "WHERE tsr.toggle.name = :toggleName AND tsr.stage.name IN :stageNames",
            ToggleStageRule.class)
            .setParameter("toggleName", toggleName)
            .setParameter("stageNames", stageChain)
            .getResultList();

        if (all.isEmpty()) {
            return all;
        }

        // Sort by inheritance chain order (closest stage first), then by priority
        Map<String, Integer> chainOrder = new HashMap<>();
        for (int i = 0; i < stageChain.size(); i++) {
            chainOrder.put(stageChain.get(i), i);
        }

        all.sort(Comparator
            .comparing((ToggleStageRule tsr) -> chainOrder.getOrDefault(tsr.getStage().getName(), Integer.MAX_VALUE))
            .thenComparing(ToggleStageRule::getPriority));

        // Only return assignments from the closest stage in the chain
        String closestStage = all.get(0).getStage().getName();
        return all.stream()
            .filter(tsr -> tsr.getStage().getName().equals(closestStage))
            .toList();
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
     * Evict a single entry from the cache using the same key parameters as queryToggles.
     * If caching is disabled or the key is not present, this is a no-op.
     */
    public void evictFromCache(String stage, String context, String nameFilter, Boolean includeDisabled) {
        if (cacheEnabled && toggleCache != null) {
            String key = buildCacheKey(stage, context, nameFilter, includeDisabled);
            toggleCache.invalidate(key);
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
        
        List<String> stageChain = List.of(stage); // Only this stage, no inheritance
        List<QueryTSRDto> tsrDtos = buildQueryResults(toggleOpt.get(), stage, stageChain);
        if (tsrDtos == null || tsrDtos.isEmpty()) {
            return null;
        }
        Toggle toggle = toggleOpt.get();
        return new ToggleDto(
            toggle.getId(),
            toggle.getName(),
            toggle.getDescription(),
            toggle.getEnabled(),
            toggle.getContext()
        );
    }

    /**
     * Evaluate toggles against a client context dictionary with caching.
     * Returns the resolved values for each toggle based on rule matching.
     */
    @Transactional
    public EvaluatorResponse evaluateToggles(String stage, String context, String nameFilter,
                                              List<ClientContextEntry> clientContext, boolean includeDebug) {
        // Build cache key that includes all query inputs and client context
        String cacheKey = buildEvaluatorCacheKey(stage, context, nameFilter, clientContext);

        // Check cache first
        if (cacheEnabled) {
            EvaluatorResponse cached = evaluatorCache.getIfPresent(cacheKey);
            if (cached != null) {
                // Return cached result with cacheHit flag
                return new EvaluatorResponse(
                    cached.results(),
                    cached.stage(),
                    cached.nameFilter(),
                    cached.context(),
                    true,
                    cacheEnabled,
                    cacheTtlSeconds
                );
            }
        }

        // Perform actual evaluation
        EvaluatorResponse response = performEvaluation(stage, context, nameFilter, clientContext, includeDebug);

        // Cache the result
        if (cacheEnabled && response != null) {
            evaluatorCache.put(cacheKey, response);
        }

        return response;
    }

    /**
     * Evaluate toggles without caching (for management endpoints).
     */
    @Transactional
    public EvaluatorResponse evaluateTogglesWithoutCache(String stage, String context, String nameFilter,
                                                         List<ClientContextEntry> clientContext, boolean includeDebug) {
        EvaluatorResponse response = performEvaluation(stage, context, nameFilter, clientContext, includeDebug);
        // Return with cacheHit=false
        return new EvaluatorResponse(
            response.results(),
            response.stage(),
            response.nameFilter(),
            response.context(),
            false,
            cacheEnabled,
            cacheTtlSeconds
        );
    }

    private EvaluatorResponse performEvaluation(String stage, String context, String nameFilter,
                                                 List<ClientContextEntry> clientContext, boolean includeDebug) {
        // Validate stage exists
        Optional<Stage> stageOpt = stageService.findByName(stage);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("Stage not found: " + stage);
        }

        // Get inheritance chain for stage fallback
        List<String> stageChain = stageService.getInheritanceChainNames(stage);

        // Find toggles matching the criteria (include disabled toggles so they show as "off")
        List<Toggle> toggles = findToggles(context, nameFilter, true);

        // Build and evaluate results
        List<EvaluatorResultDto> results = new ArrayList<>();

        for (Toggle toggle : toggles) {
            EvaluatorResultDto result = evaluateToggle(toggle, stage, stageChain, clientContext, includeDebug);
            if (result != null) {
                results.add(result);
            }
        }

        return new EvaluatorResponse(
            results,
            stage,
            nameFilter,
            clientContext,
            false,
            cacheEnabled,
            cacheTtlSeconds
        );
    }

    private EvaluatorResultDto evaluateToggle(Toggle toggle, String stage, List<String> stageChain,
                                               List<ClientContextEntry> clientContext, boolean includeDebug) {
        // Get assignments for this toggle
        List<QueryTSRDto> assignments = buildQueryResults(toggle, stage, stageChain);
        if (assignments == null || assignments.isEmpty()) {
            return null;
        }

        // If toggle is disabled, return "off" immediately
        if (!toggle.getEnabled()) {
            String debug = includeDebug ? "Toggle is disabled" : null;
            return new EvaluatorResultDto(toggle.getName(), "off", debug);
        }

        // Sort by priority (ascending)
        List<QueryTSRDto> sorted = assignments.stream()
            .sorted(Comparator.comparingInt(QueryTSRDto::priority))
            .toList();

        // Evaluate each assignment in priority order
        for (QueryTSRDto assignment : sorted) {
            List<CriterionDto> criteria = assignment.ruleCriteria();

            if (criteria == null || criteria.isEmpty()) {
                // Catch-all rule matches unconditionally
                String debug = includeDebug ? "Priority " + assignment.priority() + " (catch-all)" : null;
                return new EvaluatorResultDto(
                    toggle.getName(),
                    assignment.value() != null ? assignment.value() : "off",
                    debug
                );
            }

            // Check if all criteria match
            boolean allMatch = true;
            for (CriterionDto criterion : criteria) {
                String clientValue = getClientContextValue(clientContext, criterion.getCriterionKey());
                if (!matchesPattern(clientValue, criterion.getCriterionValue())) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                String debug = null;
                if (includeDebug) {
                    String criteriaDesc = criteria.stream()
                        .map(c -> c.getCriterionKey() + "=" + c.getCriterionValue())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                    debug = "Priority " + assignment.priority() + " (criteria: " + criteriaDesc + ")";
                }
                return new EvaluatorResultDto(
                    toggle.getName(),
                    assignment.value() != null ? assignment.value() : "off",
                    debug
                );
            }
        }

        // No rule matched - return default "off"
        String debug = includeDebug ? "No matching rule - default" : null;
        return new EvaluatorResultDto(toggle.getName(), "off", debug);
    }

    /**
     * Get a value from the client context list by key.
     * Returns empty string if key not found (first match wins for duplicate keys).
     */
    private String getClientContextValue(List<ClientContextEntry> clientContext, String key) {
        if (clientContext == null || key == null) {
            return "";
        }
        for (ClientContextEntry entry : clientContext) {
            if (key.equals(entry.key())) {
                return entry.value() != null ? entry.value() : "";
            }
        }
        return "";
    }

    /**
     * Match a value against a pattern using regex.
     * Supports /pattern/flags syntax (e.g., /US/i) for case-insensitive matching.
     * Falls back to exact string equality if regex is invalid.
     */
    private boolean matchesPattern(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return value == null || value.isEmpty();
        }

        try {
            // Check for /pattern/flags syntax
            if (pattern.startsWith("/")) {
                int lastSlash = pattern.lastIndexOf('/');
                if (lastSlash > 0) {
                    String regex = pattern.substring(1, lastSlash);
                    String flags = pattern.substring(lastSlash + 1);
                    int regexFlags = 0;
                    if (flags.contains("i")) {
                        regexFlags |= Pattern.CASE_INSENSITIVE;
                    }
                    return Pattern.compile(regex, regexFlags).matcher(value).find();
                }
            }
            // Standard regex
            return Pattern.compile(pattern).matcher(value).find();
        } catch (PatternSyntaxException e) {
            // Fall back to exact equality
            return value.equals(pattern);
        }
    }

    private String buildEvaluatorCacheKey(String stage, String context, String nameFilter,
                                           List<ClientContextEntry> clientContext) {
        StringBuilder key = new StringBuilder();
        key.append("eval:stage:").append(stage != null ? stage : "");
        key.append(":context:").append(context != null ? context : "");
        key.append(":filter:").append(nameFilter != null ? nameFilter : "");

        // Include client context in key (sorted by key for consistency)
        if (clientContext != null && !clientContext.isEmpty()) {
            key.append(":ctx:");
            clientContext.stream()
                .sorted(Comparator.comparing(ClientContextEntry::key, Comparator.nullsFirst(String::compareTo)))
                .forEach(e -> key.append(e.key()).append("=").append(e.value()).append(";"));
        }
        return key.toString();
    }

    /**
     * Evict a single entry from the evaluator cache.
     */
    public void evictFromEvaluatorCache(String stage, String context, String nameFilter,
                                       List<ClientContextEntry> clientContext) {
        if (cacheEnabled && evaluatorCache != null) {
            String key = buildEvaluatorCacheKey(stage, context, nameFilter, clientContext);
            evaluatorCache.invalidate(key);
        }
    }

    /**
     * Get evaluator cache statistics for monitoring
     */
    public Map<String, Object> getEvaluatorCacheStats() {
        if (!cacheEnabled || evaluatorCache == null) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("enabled", false);
            return stats;
        }

        com.google.common.cache.CacheStats stats = evaluatorCache.stats();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", true);
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("hitRate", stats.hitRate());
        result.put("requestCount", stats.requestCount());
        result.put("size", evaluatorCache.size());
        result.put("maxSize", cacheMaxSizeMb * 1024 * 1024L);
        result.put("ttlSeconds", cacheTtlSeconds);

        return result;
    }
}
