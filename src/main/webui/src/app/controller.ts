import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Config, ModelService, Rule, Stage, Toggle, ToggleDto, ToggleQueryResponse, ToggleStageRule } from './model.service';

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
  async createStage(name: string, description: string, displayOrder: number, parentStageName?: string): Promise<Stage> {
    try {
      const response = await firstValueFrom(
        this.http.post<Stage>('/api/stages', {
          name,
          description,
          displayOrder,
          parentStageName
        })
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
  async updateStage(name: string, newName: string, description: string, displayOrder: number, parentStageName?: string): Promise<Stage> {
    try {
      const response = await firstValueFrom(
        this.http.put<Stage>(`/api/stages/${name}`, {
          name: newName,
          description,
          displayOrder,
          parentStageName
        })
      );
      this.loadStages();
      return response;
    } catch (error) {
      console.error('Error updating stage:', error);
      throw error;
    }
  }

  /**
   * Deletes a stage by name and refreshes the stage list.
   */
  async deleteStage(name: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/stages/${name}`)
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
   * Creates a new toggle and refreshes the toggle list.
   */
  async createToggle(name: string, description: string, enabled: boolean, context: string): Promise<Toggle> {
    try {
      const response = await firstValueFrom(
        this.http.post<Toggle>('/api/toggles', {
          name,
          description,
          enabled,
          context
        })
      );
      this.loadToggles();
      return response;
    } catch (error) {
      console.error('Error creating toggle:', error);
      throw error;
    }
  }

  /**
   * Updates toggle metadata and refreshes the toggle list.
   */
  async updateToggle(name: string, description: string, enabled: boolean, context: string): Promise<Toggle> {
    try {
      const response = await firstValueFrom(
        this.http.put<Toggle>(`/api/toggles/${name}`, {
          description,
          enabled,
          context
        })
      );
      this.loadToggles();
      return response;
    } catch (error) {
      console.error('Error updating toggle:', error);
      throw error;
    }
  }

  /**
   * Deletes a toggle by name and refreshes the toggle list.
   */
  async deleteToggle(name: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/toggles/${name}`)
      );
      this.loadToggles();
    } catch (error) {
      console.error('Error deleting toggle:', error);
      throw error;
    }
  }

  /**
   * Queries toggles for a given stage and optional name filter.
   * This endpoint is public and does not require authentication.
   */
  async queryToggles(stage: string, context: string, nameFilter?: string): Promise<ToggleQueryResponse> {
    try {
      let url = `/public/toggles?stage=${encodeURIComponent(stage)}&context=${encodeURIComponent(context)}`;
      if (nameFilter) {
        url += `&nameFilter=${encodeURIComponent(nameFilter)}`;
      }
      return await firstValueFrom(this.http.get<ToggleQueryResponse>(url));
    } catch (error) {
      console.error('Error querying toggles:', error);
      throw error;
    }
  }

  /**
   * Returns the list of stage names assigned to a toggle.
   */
  async getStagesForToggle(toggleName: string): Promise<string[]> {
    try {
      const response = await firstValueFrom(
        this.http.get<string[]>(`/api/toggles/${toggleName}/stages`)
      );
      return response;
    } catch (error) {
      console.error('Error loading stages for toggle:', error);
      throw error;
    }
  }

  /**
   * Assigns a stage to a toggle.
   */
  async addStageToToggle(toggleName: string, stageName: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>(`/api/toggles/${toggleName}/stages/${stageName}`, {})
      );
    } catch (error) {
      console.error('Error adding stage to toggle:', error);
      throw error;
    }
  }

  /**
   * Removes a stage assignment from a toggle.
   */
  async removeStageFromToggle(toggleName: string, stageName: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/toggles/${toggleName}/stages/${stageName}`)
      );
    } catch (error) {
      console.error('Error removing stage from toggle:', error);
      throw error;
    }
  }

  /**
   * Loads the rules assigned to a toggle within a specific stage.
   */
  async getRulesForToggle(toggleName: string, stageName: string): Promise<Rule[]> {
    try {
      const response = await firstValueFrom(
        this.http.get<Rule[]>(`/api/toggles/${toggleName}/stages/${stageName}/rules`)
      );
      return response;
    } catch (error) {
      console.error('Error loading rules:', error);
      throw error;
    }
  }

  /**
   * Creates a new rule and assigns it to a toggle+stage.
   */
  async createRule(
    toggleName: string,
    stageName: string,
    ruleValue: string,
    priority: number,
    description: string,
    criteria: { [key: string]: string }
  ): Promise<Rule> {
    try {
      const response = await firstValueFrom(
        this.http.post<Rule>(`/api/toggles/${toggleName}/stages/${stageName}/rules`, {
          ruleValue,
          priority,
          description,
          criteria
        })
      );
      return response;
    } catch (error) {
      console.error('Error creating rule:', error);
      throw error;
    }
  }

  /**
   * Updates an existing rule's value, priority, description, and criteria.
   */
  async updateRule(
    toggleName: string,
    stageName: string,
    ruleId: string,
    ruleValue: string,
    priority: number,
    description: string,
    criteria: { [key: string]: string }
  ): Promise<Rule> {
    try {
      const response = await firstValueFrom(
        this.http.put<Rule>(`/api/toggles/${toggleName}/stages/${stageName}/rules/${ruleId}`, {
          ruleValue,
          priority,
          description,
          criteria
        })
      );
      return response;
    } catch (error) {
      console.error('Error updating rule:', error);
      throw error;
    }
  }

  /**
   * Deletes a rule from a toggle+stage.
   */
  async deleteRule(toggleName: string, stageName: string, ruleId: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/toggles/${toggleName}/stages/${stageName}/rules/${ruleId}`)
      );
    } catch (error) {
      console.error('Error deleting rule:', error);
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
  async createStandaloneRule(name: string, description: string, criteria: { [key: string]: string }): Promise<Rule> {
    try {
      const response = await firstValueFrom(
        this.http.post<Rule>('/api/rules', {
          name,
          description,
          criteria
        })
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
  async updateStandaloneRule(id: string, name: string, description: string, criteria: { [key: string]: string }): Promise<Rule> {
    try {
      const response = await firstValueFrom(
        this.http.put<Rule>(`/api/rules/${id}`, {
          name,
          description,
          criteria
        })
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
  async deleteStandaloneRule(id: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/rules/${id}`)
      );
      this.loadRules();
    } catch (error) {
      console.error('Error deleting rule:', error);
      throw error;
    }
  }

  // Toggle Stage Rule Management

  /**
   * Loads all ToggleStageRule assignments for a toggle.
   */
  async getToggleStageRules(toggleName: string): Promise<ToggleStageRule[]> {
    try {
      const response = await firstValueFrom(
        this.http.get<ToggleStageRule[]>(`/api/toggles/${toggleName}/stage-rules`)
      );
      return response;
    } catch (error) {
      console.error('Error loading toggle stage rules:', error);
      throw error;
    }
  }

  /**
   * Creates a new ToggleStageRule assignment (stage + rule + priority).
   */
  async createToggleStageRule(
    toggleName: string,
    stageName: string,
    ruleId: string,
    priority: number,
    ruleValue: string
  ): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>(`/api/toggles/${toggleName}/stage-rules`, {
          stageName,
          ruleId,
          priority,
          ruleValue
        })
      );
    } catch (error) {
      console.error('Error creating toggle stage rule:', error);
      throw error;
    }
  }

  /**
   * Updates the priority of an existing ToggleStageRule assignment.
   */
  async updateToggleStageRule(
    toggleName: string,
    id: string,
    priority: number,
    ruleValue: string
  ): Promise<ToggleStageRule> {
    try {
      const response = await firstValueFrom(
        this.http.put<ToggleStageRule>(`/api/toggles/${toggleName}/stage-rules/${id}`, {
          priority,
          ruleValue
        })
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
  async deleteToggleStageRule(toggleName: string, id: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.delete<void>(`/api/toggles/${toggleName}/stage-rules/${id}`)
      );
    } catch (error) {
      console.error('Error deleting toggle stage rule:', error);
      throw error;
    }
  }
}
