package dev.abstratium.abstoggle.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.abstratium.abstoggle.dto.ToggleQueryResponse;
import dev.abstratium.abstoggle.entity.Stage;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class ToggleQueryServiceCacheTest {

    @Inject
    ToggleQueryService toggleQueryService;

    @Inject
    EntityManager em;

    @BeforeEach
    @TestTransaction
    void setup() {
        // Clear cache before each test
        toggleQueryService.clearCache();

        // Ensure default stages exist
        if (em.createQuery("SELECT COUNT(s) FROM Stage s", Long.class).getSingleResult() == 0) {
            Stage prod = new Stage();
            prod.setName("prod");
            prod.setDescription("Production");
            prod.setDisplayOrder(3);
            em.persist(prod);
        }
    }

    @Test
    @TestTransaction
    void testCachedQueryReturnsCacheHit() {
        // First query - should be cache miss
        ToggleQueryResponse response1 = toggleQueryService.queryToggles("prod", null, null, false);
        assertNotNull(response1);
        assertNotNull(response1.getQueryMetadata());
        assertFalse(response1.getQueryMetadata().getCacheHit(), "First query should be cache miss");

        // Second query with same parameters - should be cache hit
        ToggleQueryResponse response2 = toggleQueryService.queryToggles("prod", null, null, false);
        assertNotNull(response2);
        assertNotNull(response2.getQueryMetadata());
        assertTrue(response2.getQueryMetadata().getCacheHit(), "Second query should be cache hit");
    }

    @Test
    @TestTransaction
    void testQueryWithoutCacheNeverReturnsCacheHit() {
        // First query without cache - should always be cache miss
        ToggleQueryResponse response1 = toggleQueryService.queryTogglesWithoutCache("prod", null, null, false);
        assertNotNull(response1);
        assertNotNull(response1.getQueryMetadata());
        assertFalse(response1.getQueryMetadata().getCacheHit(), "Non-cached query should never be cache hit");

        // Second query without cache - should still be cache miss
        ToggleQueryResponse response2 = toggleQueryService.queryTogglesWithoutCache("prod", null, null, false);
        assertNotNull(response2);
        assertNotNull(response2.getQueryMetadata());
        assertFalse(response2.getQueryMetadata().getCacheHit(), "Non-cached query should always be cache miss");
    }

    @Test
    @TestTransaction
    void testCachedQueryDoesNotAffectNonCachedQuery() {
        // Query with caching
        toggleQueryService.queryToggles("prod", null, null, false);

        // Query without caching - should not use cache
        ToggleQueryResponse response = toggleQueryService.queryTogglesWithoutCache("prod", null, null, false);
        assertFalse(response.getQueryMetadata().getCacheHit(), "Non-cached query should not use cached data");
    }

    @Test
    @TestTransaction
    void testDifferentCacheKeysAreIndependent() {
        // Query with different parameters should have different cache entries
        ToggleQueryResponse response1 = toggleQueryService.queryToggles("prod", null, "feature-.*", false);
        assertFalse(response1.getQueryMetadata().getCacheHit(), "First query with filter should be cache miss");

        ToggleQueryResponse response2 = toggleQueryService.queryToggles("prod", null, null, false);
        assertFalse(response2.getQueryMetadata().getCacheHit(), "Query without filter should be cache miss");

        // Query again with filter - should be cache hit now
        ToggleQueryResponse response3 = toggleQueryService.queryToggles("prod", null, "feature-.*", false);
        assertTrue(response3.getQueryMetadata().getCacheHit(), "Second query with same filter should be cache hit");
    }

    @Test
    @TestTransaction
    void testCacheStats() {
        // Clear cache to get fresh stats
        toggleQueryService.clearCache();

        // Get initial stats - need to trigger cache creation first
        toggleQueryService.queryToggles("prod", null, null, false);
        var stats1 = toggleQueryService.getCacheStats();
        assertTrue((Boolean) stats1.get("enabled"), "Cache should be enabled");

        // Make another query that should hit cache
        toggleQueryService.queryToggles("prod", null, null, false);

        // Get updated stats
        var stats2 = toggleQueryService.getCacheStats();
        assertTrue((Long) stats2.get("hitCount") >= 1L, "Should have at least 1 cache hit");
        assertTrue((Long) stats2.get("missCount") >= 1L, "Should have at least 1 cache miss");
        assertTrue((Double) stats2.get("hitRate") > 0.0, "Hit rate should be greater than 0");
    }
}
