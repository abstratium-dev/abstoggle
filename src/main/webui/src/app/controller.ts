import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Config, Criterion, EntityRevision, HistoryChange, HistoryEntry, ModelService, Rule, Stage, Toggle, ToggleDto, ToggleQueryResponse, ToggleStageRule } from './model.service';

/**
 * Service that coordinates HTTP requests to the backend toggle API
 * and pushes the resulting state into {@link ModelService}.
 */
@Injectable({
  providedIn: 'root',
})
export class Controller {

  private modelService = inject(ModelService);
  private http = inject(HttpClient);

  /**
   * Loads public application configuration from the server.
   */
  async loadConfig(): Promise<Config> {
    try {
      const config = await firstValueFrom(
        this.http.get<Config>('/public/config')
      );
      this.modelService.setConfig(config);
      return config;
    } catch (error) {
      console.error('Error loading config:', error);
      throw error;
    }
  }

  /**
   * Loads all stages from the server and updates the model.
   */
  loadStages() {
    this.modelService.setStagesLoading(true);
    this.modelService.setStagesError(null);

    this.http.get<Stage[]>('/api/stages').subscribe({
      next: (stages) => {
        this.modelService.setStages(stages);
        this.modelService.setStagesLoading(false);
      },
      error: (err) => {
        console.error('Error loading stages:', err);
        this.modelService.setStages([]);
        this.modelService.setStagesError('Failed to load stages');
        this.modelService.setStagesLoading(false);
      }
    });
  }

  /**
   * Creates a new stage and refreshes the stage list.
   */
  async createStage(name: string, description: string, displayOrder: number, parentStageName: string | undefined, changeNote: string): Promise<Stage> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.post<Stage>('/api/stages', {
          name,
          description,
          displayOrder,
          parentStageName
        }, { params })
      );
      this.loadStages();
      return response;
    } catch (error) {
      console.error('Error creating stage:', error);
      throw error;
    }
  }

  /**
   * Updates an existing stage and refreshes the stage list.
   */
  async updateStage(id: string, newName: string, description: string, displayOrder: number, parentStageName: string | undefined, changeNote: string): Promise<Stage> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.put<Stage>(`/api/stages/${id}`, {
          name: newName,
          description,
          displayOrder,
          parentStageName
        }, { params })
      );
      this.loadStages();
      return response;
    } catch (error) {
      console.error('Error updating stage:', error);
      throw error;
    }
  }

  /**
   * Deletes a stage by ID and refreshes the stage list.
   */
  async deleteStage(id: string, changeNote: string): Promise<void> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      await firstValueFrom(
        this.http.delete<void>(`/api/stages/${id}`, { params })
      );
      this.loadStages();
    } catch (error) {
      console.error('Error deleting stage:', error);
      throw error;
    }
  }

  /**
   * Loads all toggles from the server and updates the model.
   * Optionally filters by stage and/or rule assignment.
   */
  loadToggles(assignedToStage?: string, assignedToRule?: string) {
    this.modelService.setTogglesLoading(true);
    this.modelService.setTogglesError(null);

    let params = new HttpParams();
    if (assignedToStage) {
      params = params.set('assignedToStage', assignedToStage);
    }
    if (assignedToRule) {
      params = params.set('assignedToRule', assignedToRule);
    }

    this.http.get<Toggle[]>('/api/toggles/all', { params }).subscribe({
      next: (toggles) => {
        this.modelService.setToggles(toggles);
        this.modelService.setTogglesLoading(false);
      },
      error: (err) => {
        console.error('Error loading toggles:', err);
        this.modelService.setToggles([]);
        this.modelService.setTogglesError('Failed to load toggles');
        this.modelService.setTogglesLoading(false);
      }
    });
  }

  /**
   * Loads the list of distinct toggle context values from the server.
   */
  loadToggleContexts() {
    this.http.get<string[]>('/api/toggles/contexts').subscribe({
      next: (contexts) => {
        this.modelService.setToggleContexts(contexts);
      },
      error: (err) => {
        console.error('Error loading toggle contexts:', err);
      }
    });
  }

  /**
   * Creates a new toggle and refreshes the toggle list.
   */
  async createToggle(name: string, description: string, enabled: boolean, context: string, changeNote: string): Promise<Toggle> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.post<Toggle>('/api/toggles', {
          name,
          description,
          enabled,
          context
        }, { params })
      );
      this.loadToggles();
      this.loadToggleContexts();
      return response;
    } catch (error) {
      console.error('Error creating toggle:', error);
      throw error;
    }
  }

  /**
   * Updates toggle metadata and refreshes the toggle list.
   */
  async updateToggle(id: string, name: string, description: string, enabled: boolean, context: string, changeNote: string): Promise<Toggle> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.put<Toggle>(`/api/toggles/${id}`, {
          name,
          description,
          enabled,
          context
        }, { params })
      );
      this.loadToggles();
      this.loadToggleContexts();
      return response;
    } catch (error) {
      console.error('Error updating toggle:', error);
      throw error;
    }
  }

  /**
   * Deletes a toggle by ID and refreshes the toggle list.
   */
  async deleteToggle(id: string, changeNote: string): Promise<void> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      await firstValueFrom(
        this.http.delete<void>(`/api/toggles/${id}`, { params })
      );
      this.loadToggles();
      this.loadToggleContexts();
    } catch (error) {
      console.error('Error deleting toggle:', error);
      throw error;
    }
  }

  /**
   * Evicts a specific entry from the server-side toggle query cache.
   * Uses the same parameters as queryToggles to reconstruct the cache key.
   */
  async evictCache(stage: string, context: string, nameFilter?: string): Promise<void> {
    try {
      let params = new HttpParams()
        .set('stage', stage)
        .set('context', context);
      if (nameFilter) {
        params = params.set('nameFilter', nameFilter);
      }
      await firstValueFrom(
        this.http.delete<void>('/api/query/toggles/cache', { params })
      );
    } catch (error) {
      console.error('Error evicting cache:', error);
      throw error;
    }
  }

  /**
   * Queries toggles using the management API (authenticated).
   * Uses ToggleQueryService directly without caching for fresh results.
   * This works even when the public API is disabled.
   */
  async queryTogglesManagement(stage: string, context: string, nameFilter?: string): Promise<ToggleQueryResponse> {
    try {
      let url = `/api/toggles?stage=${encodeURIComponent(stage)}&context=${encodeURIComponent(context)}`;
      if (nameFilter) {
        url += `&nameFilter=${encodeURIComponent(nameFilter)}`;
      }
      return await firstValueFrom(this.http.get<ToggleQueryResponse>(url));
    } catch (error) {
      console.error('Error querying toggles via management API:', error);
      throw error;
    }
  }

  // Reusable Rule Management

  /**
   * Loads all reusable rules from the server and updates the model.
   */
  loadRules() {
    this.modelService.setRulesLoading(true);
    this.modelService.setRulesError(null);

    this.http.get<Rule[]>('/api/rules').subscribe({
      next: (rules) => {
        this.modelService.setRules(rules);
        this.modelService.setRulesLoading(false);
      },
      error: (err) => {
        console.error('Error loading rules:', err);
        this.modelService.setRules([]);
        this.modelService.setRulesError('Failed to load rules');
        this.modelService.setRulesLoading(false);
      }
    });
  }

  /**
   * Creates a new reusable rule and refreshes the rule list.
   */
  async createStandaloneRule(name: string, description: string, criteria: Criterion[], changeNote: string): Promise<Rule> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.post<Rule>('/api/rules', {
          name,
          description,
          criteria
        }, { params })
      );
      this.loadRules();
      return response;
    } catch (error) {
      console.error('Error creating rule:', error);
      throw error;
    }
  }

  /**
   * Updates an existing reusable rule and refreshes the rule list.
   */
  async updateStandaloneRule(id: string, name: string, description: string, criteria: Criterion[], changeNote: string): Promise<Rule> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.put<Rule>(`/api/rules/${id}`, {
          name,
          description,
          criteria
        }, { params })
      );
      this.loadRules();
      return response;
    } catch (error) {
      console.error('Error updating rule:', error);
      throw error;
    }
  }

  /**
   * Deletes a reusable rule by ID and refreshes the rule list.
   */
  async deleteStandaloneRule(id: string, changeNote: string): Promise<void> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      await firstValueFrom(
        this.http.delete<void>(`/api/rules/${id}`, { params })
      );
      this.loadRules();
    } catch (error) {
      console.error('Error deleting rule:', error);
      throw error;
    }
  }

  // Toggle Stage Rule Management

  /**
   * Loads all ToggleStageRule assignments for a toggle (by toggle UUID).
   */
  async getToggleStageRules(toggleId: string): Promise<ToggleStageRule[]> {
    try {
      const response = await firstValueFrom(
        this.http.get<ToggleStageRule[]>('/api/toggle-stage-rules', {
          params: { toggleId }
        })
      );
      return response;
    } catch (error) {
      console.error('Error loading toggle stage rules:', error);
      throw error;
    }
  }

  /**
   * Creates a new ToggleStageRule assignment (toggleId + stageId + ruleId + priority + toggleValue).
   */
  async createToggleStageRule(
    toggleId: string,
    stageId: string,
    ruleId: string,
    priority: number,
    toggleValue: string,
    changeNote: string
  ): Promise<ToggleStageRule> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.post<ToggleStageRule>('/api/toggle-stage-rules', {
          toggleId,
          stageId,
          ruleId,
          priority,
          toggleValue
        }, { params })
      );
      return response;
    } catch (error) {
      console.error('Error creating toggle stage rule:', error);
      throw error;
    }
  }

  /**
   * Updates the toggleValue and priority of an existing ToggleStageRule assignment.
   */
  async updateToggleStageRule(
    id: string,
    toggleValue: string,
    priority: number,
    changeNote: string
  ): Promise<ToggleStageRule> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      const response = await firstValueFrom(
        this.http.put<ToggleStageRule>(`/api/toggle-stage-rules/${id}`, {
          toggleValue,
          priority
        }, { params })
      );
      return response;
    } catch (error) {
      console.error('Error updating toggle stage rule:', error);
      throw error;
    }
  }

  /**
   * Deletes a ToggleStageRule assignment by ID.
   */
  async deleteToggleStageRule(id: string, changeNote: string): Promise<void> {
    try {
      const params = new HttpParams().set('changeNote', changeNote);
      await firstValueFrom(
        this.http.delete<void>(`/api/toggle-stage-rules/${id}`, { params })
      );
    } catch (error) {
      console.error('Error deleting toggle stage rule:', error);
      throw error;
    }
  }

  // History

  /**
   * Searches revision history, optionally filtering by username or change note.
   */
  async loadHistory(search?: string, limit = 50, offset = 0): Promise<HistoryEntry[]> {
    try {
      let params = new HttpParams()
        .set('limit', limit.toString())
        .set('offset', offset.toString());
      if (search && search.trim()) {
        params = params.set('search', search.trim());
      }
      return await firstValueFrom(
        this.http.get<HistoryEntry[]>('/api/history', { params })
      );
    } catch (error) {
      console.error('Error loading history:', error);
      throw error;
    }
  }

  /**
   * Returns entity-level changes for a specific revision number.
   */
  async getRevisionDetails(rev: number): Promise<HistoryChange[]> {
    try {
      return await firstValueFrom(
        this.http.get<HistoryChange[]>(`/api/history/${rev}`)
      );
    } catch (error) {
      console.error('Error loading revision details:', error);
      throw error;
    }
  }

  /**
   * Returns the full audit history for a specific entity.
   */
  async getEntityHistory(table: string, entityId: string): Promise<EntityRevision[]> {
    try {
      return await firstValueFrom(
        this.http.get<EntityRevision[]>(`/api/history/entity/${encodeURIComponent(table)}/${encodeURIComponent(entityId)}`)
      );
    } catch (error) {
      console.error('Error loading entity history:', error);
      throw error;
    }
  }
}
