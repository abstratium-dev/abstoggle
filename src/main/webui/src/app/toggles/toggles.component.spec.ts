import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TogglesComponent } from './toggles.component';
import { Controller } from '../controller';
import { ModelService, Toggle } from '../model.service';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { AuthService } from '../core/auth.service';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';

describe('TogglesComponent', () => {
  let component: TogglesComponent;
  let fixture: ComponentFixture<TogglesComponent>;
  let controller: jasmine.SpyObj<Controller>;
  let modelService: jasmine.SpyObj<ModelService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let confirmService: jasmine.SpyObj<ConfirmDialogService>;
  let authService: jasmine.SpyObj<AuthService>;

  // Store signal references for testing
  let togglesSignal: ReturnType<typeof signal<Toggle[]>>;
  let loadingSignal: ReturnType<typeof signal<boolean>>;
  let errorSignal: ReturnType<typeof signal<string | null>>;

  const mockToggles: Toggle[] = [
    { name: 'feature-a', description: 'Feature A toggle', enabled: true },
    { name: 'feature-b', description: 'Feature B toggle', enabled: false },
    { name: 'feature-c', enabled: true }
  ];

  beforeEach(async () => {
    const controllerSpy = jasmine.createSpyObj('Controller', [
      'loadToggles', 'loadStages', 'loadRules', 'createToggle', 'updateToggle', 'deleteToggle',
      'getToggleStageRules', 'createToggleStageRule', 'updateToggleStageRule', 'deleteToggleStageRule'
    ]);

    // Create writable signals for testing
    togglesSignal = signal<Toggle[]>([]);
    loadingSignal = signal<boolean>(false);
    errorSignal = signal<string | null>(null);
    const rulesSignal = signal<[]>([]);
    const rulesLoadingSignal = signal<boolean>(false);
    const rulesErrorSignal = signal<string | null>(null);

    const modelServiceSpy = jasmine.createSpyObj('ModelService', [], {
      toggles$: togglesSignal,
      togglesLoading$: loadingSignal,
      togglesError$: errorSignal,
      stages$: signal<[]>([]),
      rules$: rulesSignal,
      rulesLoading$: rulesLoadingSignal,
      rulesError$: rulesErrorSignal
    });
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const confirmServiceSpy = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getEmail']);
    authServiceSpy.getEmail.and.returnValue('test@example.com');

    await TestBed.configureTestingModule({
      imports: [TogglesComponent],
      providers: [
        provideRouter([]),
        { provide: Controller, useValue: controllerSpy },
        { provide: ModelService, useValue: modelServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ConfirmDialogService, useValue: confirmServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    controller = TestBed.inject(Controller) as jasmine.SpyObj<Controller>;
    modelService = TestBed.inject(ModelService) as jasmine.SpyObj<ModelService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    confirmService = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    fixture = TestBed.createComponent(TogglesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load toggles, stages and rules on init', () => {
      fixture.detectChanges();
      expect(controller.loadToggles).toHaveBeenCalled();
      expect(controller.loadStages).toHaveBeenCalled();
      expect(controller.loadRules).toHaveBeenCalled();
    });
  });

  describe('Form Management', () => {
    it('should toggle add form', () => {
      expect(component.showAddForm).toBe(false);

      component.toggleAddForm();
      expect(component.showAddForm).toBe(true);

      component.toggleAddForm();
      expect(component.showAddForm).toBe(false);
    });

    it('should reset form when opening', () => {
      component.toggleName = 'Test';
      component.toggleDescription = 'Description';
      component.toggleEnabled = false;
      component.formError = 'Some error';

      component.toggleAddForm();

      expect(component.toggleName).toBe('');
      expect(component.toggleDescription).toBe('');
      expect(component.toggleEnabled).toBe(true);
      expect(component.formError).toBeNull();
    });

    it('should start editing a toggle', () => {
      const toggle: Toggle = mockToggles[0];

      component.startEdit(toggle);

      expect(component.editingToggle).toBe(toggle);
      expect(component.showAddForm).toBe(true);
      expect(component.toggleName).toBe(toggle.name);
      expect(component.toggleDescription).toBe(toggle.description || '');
      expect(component.toggleEnabled).toBe(toggle.enabled ?? true);
    });

    it('should cancel editing', () => {
      component.editingToggle = mockToggles[0];
      component.showAddForm = true;
      component.toggleName = 'Test';

      component.cancelEdit();

      expect(component.editingToggle).toBeNull();
      expect(component.showAddForm).toBe(false);
      expect(component.toggleName).toBe('');
    });

    it('should reset form fields', () => {
      component.toggleName = 'Test';
      component.toggleDescription = 'Description';
      component.toggleEnabled = false;
      component.formError = 'Error';

      component.resetForm();

      expect(component.toggleName).toBe('');
      expect(component.toggleDescription).toBe('');
      expect(component.toggleEnabled).toBe(true);
      expect(component.formError).toBeNull();
    });
  });

  describe('Create Toggle', () => {
    it('should require toggle name', async () => {
      component.toggleName = '';

      await component.onSubmit();

      expect(component.formError).toBe('Toggle name is required');
      expect(controller.createToggle).not.toHaveBeenCalled();
    });

    it('should create toggle successfully', async () => {
      const newToggle: Toggle = {
        name: 'new-toggle',
        description: 'New Description',
        enabled: true
      };
      controller.createToggle.and.returnValue(Promise.resolve(newToggle));

      component.toggleName = 'new-toggle';
      component.toggleDescription = 'New Description';

      await component.onSubmit();

      expect(controller.createToggle).toHaveBeenCalledWith(
        'new-toggle',
        'New Description',
        true,
        ''
      );
      expect(toastService.success).toHaveBeenCalledWith('Toggle created successfully');
      expect(component.showAddForm).toBe(false);
      expect(component.formSubmitting).toBe(false);
    });

    it('should create toggle without description', async () => {
      const newToggle: Toggle = {
        name: 'minimal-toggle',
        enabled: true
      };
      controller.createToggle.and.returnValue(Promise.resolve(newToggle));

      component.toggleName = 'minimal-toggle';
      component.toggleDescription = '';

      await component.onSubmit();

      expect(controller.createToggle).toHaveBeenCalledWith(
        'minimal-toggle',
        '',
        true,
        ''
      );
    });

    it('should handle create error', async () => {
      controller.createToggle.and.returnValue(Promise.reject({
        error: { detail: 'Toggle already exists' }
      }));

      component.toggleName = 'Existing Toggle';

      await component.onSubmit();

      expect(controller.createToggle).toHaveBeenCalled();
      expect(component.formError).toBe('Toggle already exists');
      expect(component.formSubmitting).toBe(false);
    });

    it('should trim whitespace from toggle name', async () => {
      controller.createToggle.and.returnValue(Promise.resolve(mockToggles[0]));

      component.toggleName = '  Test Toggle  ';
      component.toggleDescription = '  Test Description  ';

      await component.onSubmit();

      expect(controller.createToggle).toHaveBeenCalledWith(
        'Test Toggle',
        'Test Description',
        true,
        ''
      );
    });
  });

  describe('Update Toggle', () => {
    it('should update toggle successfully', async () => {
      const updatedToggle: Toggle = {
        ...mockToggles[0],
        description: 'Updated Description',
        enabled: false
      };
      controller.updateToggle.and.returnValue(Promise.resolve(updatedToggle));

      component.editingToggle = mockToggles[0];
      component.toggleName = 'feature-a';
      component.toggleDescription = 'Updated Description';
      component.toggleEnabled = false;

      await component.onSubmit();

      expect(controller.updateToggle).toHaveBeenCalledWith(
        'feature-a',
        'Updated Description',
        false,
        ''
      );
      expect(toastService.success).toHaveBeenCalledWith('Toggle updated successfully');
      expect(component.showAddForm).toBe(false);
      expect(component.editingToggle).toBeNull();
    });

    it('should update toggle with enabled true', async () => {
      const updatedToggle: Toggle = {
        ...mockToggles[1],
        enabled: true
      };
      controller.updateToggle.and.returnValue(Promise.resolve(updatedToggle));

      component.editingToggle = mockToggles[1];
      component.toggleName = 'feature-b';
      component.toggleDescription = 'Feature B toggle';
      component.toggleEnabled = true;

      await component.onSubmit();

      expect(controller.updateToggle).toHaveBeenCalledWith(
        'feature-b',
        'Feature B toggle',
        true,
        ''
      );
    });
  });

  describe('Delete Toggle', () => {
    it('should delete toggle after confirmation', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(true));
      controller.deleteToggle.and.returnValue(Promise.resolve());

      await component.deleteToggle(mockToggles[0]);

      expect(confirmService.confirm).toHaveBeenCalledWith({
        title: 'Delete Toggle',
        message: 'Are you sure you want to delete the toggle "feature-a"? This action cannot be undone.',
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmClass: 'btn-danger'
      });
      expect(controller.deleteToggle).toHaveBeenCalledWith('feature-a');
      expect(toastService.success).toHaveBeenCalledWith('Toggle deleted successfully');
    });

    it('should not delete if user cancels', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(false));

      await component.deleteToggle(mockToggles[0]);

      expect(confirmService.confirm).toHaveBeenCalled();
      expect(controller.deleteToggle).not.toHaveBeenCalled();
      expect(toastService.success).not.toHaveBeenCalled();
    });

    it('should show error toast on delete failure', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(true));
      controller.deleteToggle.and.returnValue(Promise.reject({
        error: { detail: 'Cannot delete toggle with active rules' }
      }));

      await component.deleteToggle(mockToggles[0]);

      expect(controller.deleteToggle).toHaveBeenCalledWith('feature-a');
      expect(toastService.error).toHaveBeenCalledWith('Cannot delete toggle with active rules');
    });
  });

  describe('Helper Methods', () => {
    it('should return Yes for enabled toggle', () => {
      expect(component.getEnabledStatus(mockToggles[0])).toBe('Yes');
    });

    it('should return No for disabled toggle', () => {
      expect(component.getEnabledStatus(mockToggles[1])).toBe('No');
    });

    it('should return Yes for toggle with undefined enabled (defaults to true)', () => {
      const toggle: Toggle = { name: 'test' };
      expect(component.getEnabledStatus(toggle)).toBe('No');
    });
  });

  describe('Signal Integration', () => {
    it('should use toggles signal from model service', () => {
      expect(component.toggles).toBe(modelService.toggles$);
    });

    it('should use loading signal from model service', () => {
      expect(component.loading).toBe(modelService.togglesLoading$);
    });

    it('should use error signal from model service', () => {
      expect(component.error).toBe(modelService.togglesError$);
    });
  });

  describe('Toggle Stage Rule Management', () => {
    const mockStageRules = [
      { id: 'sr-1', toggleName: 'feature-a', stageName: 'dev', ruleId: 'rule-1', ruleName: 'eu-rule', ruleValue: 'on', priority: 1, criteria: {} },
      { id: 'sr-2', toggleName: 'feature-a', stageName: 'prod', ruleId: 'rule-2', ruleName: 'default-rule', ruleValue: 'off', priority: 100, criteria: {} }
    ];

    it('should load stage rules when starting management', async () => {
      controller.getToggleStageRules.and.returnValue(Promise.resolve(mockStageRules));

      await component.startManageStageRules(mockToggles[0]);

      expect(component.managingToggle).toBe(mockToggles[0]);
      expect(controller.getToggleStageRules).toHaveBeenCalledWith('feature-a');
      expect(component.toggleStageRules).toEqual(mockStageRules);
      expect(component.stageRulesLoading).toBe(false);
    });

    it('should handle error loading stage rules', async () => {
      controller.getToggleStageRules.and.returnValue(Promise.reject({
        error: { detail: 'Toggle not found' }
      }));

      await component.startManageStageRules(mockToggles[0]);

      expect(toastService.error).toHaveBeenCalledWith('Toggle not found');
      expect(component.stageRulesLoading).toBe(false);
    });

    it('should cancel management and clear state', () => {
      component.managingToggle = mockToggles[0];
      component.toggleStageRules = mockStageRules;

      component.cancelManageStageRules();

      expect(component.managingToggle).toBeNull();
      expect(component.toggleStageRules).toEqual([]);
    });

    it('should create stage rule successfully', async () => {
      controller.createToggleStageRule.and.returnValue(Promise.resolve());
      controller.getToggleStageRules.and.returnValue(Promise.resolve(mockStageRules));

      component.managingToggle = mockToggles[0];
      component.selectedStageName = 'dev';
      component.selectedRuleId = 'rule-1';
      component.newRulePriority = 5;

      await component.saveStageRule();

      expect(controller.createToggleStageRule).toHaveBeenCalledWith('feature-a', 'dev', 'rule-1', 5);
      expect(toastService.success).toHaveBeenCalledWith('Assignment created successfully');
      expect(component.showAddStageRuleForm).toBe(false);
    });

    it('should require stage and rule for creation', async () => {
      component.managingToggle = mockToggles[0];
      component.selectedStageName = '';
      component.selectedRuleId = '';

      await component.saveStageRule();

      expect(controller.createToggleStageRule).not.toHaveBeenCalled();
      expect(toastService.error).toHaveBeenCalledWith('Stage and rule are required');
    });

    it('should update stage rule priority successfully', async () => {
      controller.updateToggleStageRule.and.returnValue(Promise.resolve(mockStageRules[0]));
      controller.getToggleStageRules.and.returnValue(Promise.resolve(mockStageRules));

      component.managingToggle = mockToggles[0];
      component.editingStageRule = mockStageRules[0];
      component.editRulePriority = 10;
      component.editRuleValue = 'off';

      await component.saveStageRule();

      expect(controller.updateToggleStageRule).toHaveBeenCalledWith('feature-a', 'sr-1', 10, 'off');
      expect(toastService.success).toHaveBeenCalledWith('Assignment updated successfully');
    });

    it('should delete stage rule after confirmation', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(true));
      controller.deleteToggleStageRule.and.returnValue(Promise.resolve());
      controller.getToggleStageRules.and.returnValue(Promise.resolve([]));

      component.managingToggle = mockToggles[0];
      await component.deleteStageRule(mockStageRules[0]);

      expect(confirmService.confirm).toHaveBeenCalled();
      expect(controller.deleteToggleStageRule).toHaveBeenCalledWith('feature-a', 'sr-1');
      expect(toastService.success).toHaveBeenCalledWith('Assignment deleted successfully');
    });

    it('should not delete stage rule if user cancels', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(false));

      component.managingToggle = mockToggles[0];
      await component.deleteStageRule(mockStageRules[0]);

      expect(controller.deleteToggleStageRule).not.toHaveBeenCalled();
    });

    it('should toggle add stage rule form', () => {
      expect(component.showAddStageRuleForm).toBe(false);

      component.toggleAddStageRuleForm();
      expect(component.showAddStageRuleForm).toBe(true);
      expect(component.selectedStageName).toBe('');
      expect(component.selectedRuleId).toBe('');

      component.toggleAddStageRuleForm();
      expect(component.showAddStageRuleForm).toBe(false);
    });

    it('should start editing stage rule', () => {
      component.toggleAddStageRuleForm(); // open form first
      component.startEditStageRule(mockStageRules[0]);

      expect(component.editingStageRule).toBe(mockStageRules[0]);
      expect(component.editRulePriority).toBe(1);
      expect(component.showAddStageRuleForm).toBe(true);
    });

    it('should cancel editing stage rule', () => {
      component.editingStageRule = mockStageRules[0];
      component.showAddStageRuleForm = true;

      component.cancelEditStageRule();

      expect(component.editingStageRule).toBeNull();
      expect(component.showAddStageRuleForm).toBe(false);
    });
  });

  describe('Retry', () => {
    it('should call controller loadToggles with current filters on retry', () => {
      component.filterStageName = 'dev';
      component.filterRuleName = 'eu-rule';
      component.onRetry();
      expect(controller.loadToggles).toHaveBeenCalledWith('dev', 'eu-rule');
    });
  });

  describe('Filters', () => {
    it('should call loadToggles with stage filter when filter changes', () => {
      component.filterStageName = 'prod';
      component.onFilterChange();
      expect(controller.loadToggles).toHaveBeenCalledWith('prod', undefined);
    });

    it('should call loadToggles with rule filter when filter changes', () => {
      component.filterRuleName = 'default-rule';
      component.onFilterChange();
      expect(controller.loadToggles).toHaveBeenCalledWith(undefined, 'default-rule');
    });

    it('should call loadToggles with both filters when both are set', () => {
      component.filterStageName = 'dev';
      component.filterRuleName = 'beta-rule';
      component.onFilterChange();
      expect(controller.loadToggles).toHaveBeenCalledWith('dev', 'beta-rule');
    });

    it('should clear filters and reload toggles', () => {
      component.filterStageName = 'dev';
      component.filterRuleName = 'beta-rule';
      controller.loadToggles.calls.reset();

      component.clearFilters();

      expect(component.filterStageName).toBeNull();
      expect(component.filterRuleName).toBeNull();
      expect(controller.loadToggles).toHaveBeenCalledWith(undefined, undefined);
    });
  });
});
