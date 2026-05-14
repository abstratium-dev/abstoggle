import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RulesComponent } from './rules.component';
import { ModelService, Rule } from '../model.service';
import { Controller } from '../controller';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';

describe('RulesComponent', () => {
  let component: RulesComponent;
  let fixture: ComponentFixture<RulesComponent>;
  let controller: jasmine.SpyObj<Controller>;
  let toastService: jasmine.SpyObj<ToastService>;
  let confirmService: jasmine.SpyObj<ConfirmDialogService>;

  const mockRules: Rule[] = [
    {
      id: 'rule-1',
      name: 'beta-testers',
      priority: 0,
      value: 'on',
      description: 'Beta testers rule',
      criteria: { userId: '^(alice|bob)$' }
    },
    {
      id: 'rule-2',
      name: 'catch-all',
      priority: 0,
      value: 'off',
      description: 'Default off rule',
      criteria: {}
    }
  ];

  beforeEach(async () => {
    const controllerSpy = jasmine.createSpyObj('Controller', [
      'loadRules',
      'createStandaloneRule',
      'updateStandaloneRule',
      'deleteStandaloneRule'
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const confirmSpy = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);

    await TestBed.configureTestingModule({
      imports: [RulesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Controller, useValue: controllerSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ConfirmDialogService, useValue: confirmSpy }
      ]
    }).compileComponents();

    controller = TestBed.inject(Controller) as jasmine.SpyObj<Controller>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    confirmService = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;

    fixture = TestBed.createComponent(RulesComponent);
    component = fixture.componentInstance;

    const modelService = TestBed.inject(ModelService);
    modelService.setRules(mockRules);
    modelService.setRulesLoading(false);
    modelService.setRulesError(null);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load rules on init', () => {
    fixture.detectChanges();
    expect(controller.loadRules).toHaveBeenCalled();
  });

  it('should display rules in table', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('beta-testers');
    expect(compiled.textContent).toContain('catch-all');
  });

  it('should show empty message when no rules', () => {
    const modelService = TestBed.inject(ModelService);
    modelService.setRules([]);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No reusable rules found');
  });

  it('should toggle add form', () => {
    fixture.detectChanges();
    expect(component.showAddForm).toBeFalse();
    component.toggleAddForm();
    expect(component.showAddForm).toBeTrue();
  });

  it('should reset form when toggling add form', () => {
    component.ruleName = 'test';
    component.toggleAddForm();
    expect(component.ruleName).toBe('');
  });

  it('should populate form when editing', () => {
    fixture.detectChanges();
    component.startEdit(mockRules[0]);
    expect(component.editingRule).toBe(mockRules[0]);
    expect(component.ruleName).toBe('beta-testers');
    expect(component.ruleValue).toBe('on');
    expect(component.criteriaEntries.length).toBe(1);
  });

  it('should add criterion entry', () => {
    component.criteriaKey = 'userId';
    component.criteriaValue = '^alice$';
    component.addCriterionEntry();
    expect(component.criteriaEntries.length).toBe(1);
    expect(component.criteriaEntries[0]).toEqual({ key: 'userId', value: '^alice$' });
  });

  it('should not add empty criterion entry', () => {
    component.criteriaKey = '';
    component.criteriaValue = 'value';
    component.addCriterionEntry();
    expect(component.criteriaEntries.length).toBe(0);
  });

  it('should remove criterion entry', () => {
    component.criteriaEntries = [{ key: 'a', value: 'b' }, { key: 'c', value: 'd' }];
    component.removeCriterionEntry(0);
    expect(component.criteriaEntries.length).toBe(1);
    expect(component.criteriaEntries[0]).toEqual({ key: 'c', value: 'd' });
  });

  it('should require name and value on submit', async () => {
    fixture.detectChanges();
    component.showAddForm = true;
    component.ruleName = '';
    component.ruleValue = '';
    await component.onSubmit();
    expect(component.formError).toBe('Rule name is required');
    expect(controller.createStandaloneRule).not.toHaveBeenCalled();
  });

  it('should create rule on submit', async () => {
    fixture.detectChanges();
    controller.createStandaloneRule.and.resolveTo(mockRules[0]);
    component.showAddForm = true;
    component.ruleName = 'new-rule';
    component.ruleValue = 'on';
    component.ruleDescription = 'A new rule';
    await component.onSubmit();
    expect(controller.createStandaloneRule).toHaveBeenCalledWith(
      'new-rule', 'on', 'A new rule', {}
    );
    expect(toastService.success).toHaveBeenCalledWith('Rule created successfully');
  });

  it('should update rule on submit when editing', async () => {
    fixture.detectChanges();
    controller.updateStandaloneRule.and.resolveTo(mockRules[0]);
    component.startEdit(mockRules[0]);
    component.ruleName = 'updated-name';
    await component.onSubmit();
    expect(controller.updateStandaloneRule).toHaveBeenCalledWith(
      'rule-1', 'updated-name', 'on', 'Beta testers rule', { userId: '^(alice|bob)$' }
    );
    expect(toastService.success).toHaveBeenCalledWith('Rule updated successfully');
  });

  it('should delete rule after confirmation', async () => {
    fixture.detectChanges();
    confirmService.confirm.and.resolveTo(true);
    controller.deleteStandaloneRule.and.resolveTo();
    await component.deleteRule(mockRules[0]);
    expect(confirmService.confirm).toHaveBeenCalled();
    expect(controller.deleteStandaloneRule).toHaveBeenCalledWith('rule-1');
    expect(toastService.success).toHaveBeenCalledWith('Rule deleted successfully');
  });

  it('should not delete rule if cancelled', async () => {
    fixture.detectChanges();
    confirmService.confirm.and.resolveTo(false);
    await component.deleteRule(mockRules[0]);
    expect(controller.deleteStandaloneRule).not.toHaveBeenCalled();
  });

  it('should show error toast on delete failure', async () => {
    fixture.detectChanges();
    confirmService.confirm.and.resolveTo(true);
    controller.deleteStandaloneRule.and.rejectWith({ error: { detail: 'Rule is still assigned' } });
    await component.deleteRule(mockRules[0]);
    expect(toastService.error).toHaveBeenCalledWith('Rule is still assigned');
  });

  it('should format criteria correctly', () => {
    expect(component.formatCriteria({ userId: '^alice$', env: 'prod' })).toBe('userId: ^alice$, env: prod');
    expect(component.formatCriteria({})).toBe('None (catch-all)');
    expect(component.formatCriteria(null as any)).toBe('None (catch-all)');
  });
});
