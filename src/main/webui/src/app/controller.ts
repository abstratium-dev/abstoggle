import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Config, ModelService, Rule, Stage, Toggle, ToggleDto, ToggleQueryResponse } from './model.service';

@Injectable({
  providedIn: 'root',
})
export class Controller {

  private modelService = inject(ModelService);
  private http = inject(HttpClient);

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

  loadToggles() {
    this.modelService.setTogglesLoading(true);
    this.modelService.setTogglesError(null);

    this.http.get<Toggle[]>('/api/toggles/all').subscribe({
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

  // Public toggle query (no auth required)
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

  // Toggle Stage Management
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

  async addStageToToggle(toggleName: string, stageName: string): Promise<string> {
    try {
      const response = await firstValueFrom(
        this.http.post<string>(`/api/toggles/${toggleName}/stages/${stageName}`, {})
      );
      return response;
    } catch (error) {
      console.error('Error adding stage to toggle:', error);
      throw error;
    }
  }

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

  // Toggle Rule Management
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

  async assignExistingRule(
    toggleName: string,
    stageName: string,
    ruleId: string,
    priority?: number
  ): Promise<Rule> {
    try {
      const response = await firstValueFrom(
        this.http.post<Rule>(
          `/api/toggles/${toggleName}/stages/${stageName}/rules/existing/${ruleId}`,
          priority !== undefined ? { priority } : {}
        )
      );
      return response;
    } catch (error) {
      console.error('Error assigning existing rule:', error);
      throw error;
    }
  }

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
}
