import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RulesComponent } from './rules.component';
import { Criterion, ModelService, Rule } from '../model.service';
import { Controller } from '../controller';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { DeleteConfirmDialogService } from '../core/delete-confirm-dialog/delete-confirm-dialog.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

describe('RulesComponent', () => {
  let component: RulesComponent;
  let fixture: ComponentFixture<RulesComponent>;
  let controller: jasmine.SpyObj<Controller>;
  let toastService: jasmine.SpyObj<ToastService>;
  let confirmService: jasmine.SpyObj<ConfirmDialogService>;
  let deleteConfirmDialog: jasmine.SpyObj<DeleteConfirmDialogService>;
  let router: jasmine.SpyObj<Router>;

  const mockRules: Rule[] = [
    {
      id: 'rule-1',
      name: 'beta-testers',
      description: 'Beta testers rule',
      criteria: [{ criterionKey: 'userId', criterionValue: '^(alice|bob)$' }]
    },
    {
      id: 'rule-2',
      name: 'catch-all',
      description: 'Default off rule',
      criteria: []
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
    const deleteConfirmDialogSpy = jasmine.createSpyObj('DeleteConfirmDialogService', ['confirm']);
    deleteConfirmDialogSpy.confirm.and.returnValue(Promise.resolve('Test change note'));
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RulesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Controller, useValue: controllerSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ConfirmDialogService, useValue: confirmSpy },
        { provide: DeleteConfirmDialogService, useValue: deleteConfirmDialogSpy },
        { provide: ActivatedRoute, useValue: { queryParamMap: of({ get: () => null }) } },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    controller = TestBed.inject(Controller) as jasmine.SpyObj<Controller>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    confirmService = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;
    deleteConfirmDialog = TestBed.inject(DeleteConfirmDialogService) as jasmine.SpyObj<DeleteConfirmDialogService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

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
    component.changeNote = 'test note';
    component.toggleAddForm();
    expect(component.ruleName).toBe('');
    expect(component.changeNote).toBe('');
  });

  it('should populate form when editing', () => {
    fixture.detectChanges();
    component.startEdit(mockRules[0]);
    expect(component.editingRule).toBe(mockRules[0]);
    expect(component.ruleName).toBe('beta-testers');
    expect(component.criteriaEntries.length).toBe(1);
  });

  it('should add criterion entry', () => {
    component.criteriaKey = 'userId';
    component.criteriaValue = '^alice$';
    component.addCriterionEntry();
    expect(component.criteriaEntries.length).toBe(1);
    expect(component.criteriaEntries[0]).toEqual({ criterionKey: 'userId', criterionValue: '^alice$' });
  });

  it('should not add empty criterion entry', () => {
    component.criteriaKey = '';
    component.criteriaValue = 'value';
    component.addCriterionEntry();
    expect(component.criteriaEntries.length).toBe(0);
  });

  it('should remove criterion entry', () => {
    component.criteriaEntries = [
      { criterionKey: 'a', criterionValue: 'b' },
      { criterionKey: 'c', criterionValue: 'd' }
    ];
    component.removeCriterionEntry(0);
    expect(component.criteriaEntries.length).toBe(1);
    expect(component.criteriaEntries[0]).toEqual({ criterionKey: 'c', criterionValue: 'd' });
  });

  it('should require name on submit', async () => {
    fixture.detectChanges();
    component.showAddForm = true;
    component.ruleName = '';
    await component.onSubmit();
    expect(component.formError).toBe('Rule name is required');
    expect(controller.createStandaloneRule).not.toHaveBeenCalled();
  });

  it('should create rule on submit', async () => {
    fixture.detectChanges();
    controller.createStandaloneRule.and.resolveTo(mockRules[0]);
    component.showAddForm = true;
    component.ruleName = 'new-rule';
    component.ruleDescription = 'A new rule';
    await component.onSubmit();
    expect(controller.createStandaloneRule).toHaveBeenCalledWith(
      'new-rule', 'A new rule', [], ''
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
      'rule-1', 'updated-name', 'Beta testers rule',
      [{ criterionKey: 'userId', criterionValue: '^(alice|bob)$' }], ''
    );
    expect(toastService.success).toHaveBeenCalledWith('Rule updated successfully');
  });

  it('should delete rule after confirmation', async () => {
    fixture.detectChanges();
    deleteConfirmDialog.confirm.and.resolveTo('Test change note');
    controller.deleteStandaloneRule.and.resolveTo();
    await component.deleteRule(mockRules[0]);
    expect(deleteConfirmDialog.confirm).toHaveBeenCalled();
    expect(controller.deleteStandaloneRule).toHaveBeenCalledWith('rule-1', 'Test change note');
    expect(toastService.success).toHaveBeenCalledWith('Rule deleted successfully');
  });

  it('should not delete rule if cancelled', async () => {
    fixture.detectChanges();
    deleteConfirmDialog.confirm.and.resolveTo(null);
    await component.deleteRule(mockRules[0]);
    expect(controller.deleteStandaloneRule).not.toHaveBeenCalled();
  });

  it('should show error toast on delete failure', async () => {
    fixture.detectChanges();
    deleteConfirmDialog.confirm.and.resolveTo('Test change note');
    controller.deleteStandaloneRule.and.rejectWith({ error: { detail: 'Rule is still assigned' } });
    await component.deleteRule(mockRules[0]);
    expect(controller.deleteStandaloneRule).toHaveBeenCalledWith('rule-1', 'Test change note');
    expect(toastService.error).toHaveBeenCalledWith('Rule is still assigned');
  });

  it('should format criteria correctly', () => {
    const criteria: Criterion[] = [
      { criterionKey: 'userId', criterionValue: '^alice$' },
      { criterionKey: 'env', criterionValue: 'prod' }
    ];
    expect(component.formatCriteria(criteria)).toBe('userId: ^alice$, env: prod');
    expect(component.formatCriteria([])).toBe('None (catch-all)');
    expect(component.formatCriteria(null as any)).toBe('None (catch-all)');
  });

  describe('Navigation', () => {
    it('goToToggles should navigate to toggles filtered by rule name', () => {
      component.goToToggles(mockRules[0]);
      expect(router.navigate).toHaveBeenCalledWith(['/toggles'], { queryParams: { filterRule: 'beta-testers' } });
    });

    it('goToHistory should navigate to history with Rule entity params', () => {
      component.goToHistory(mockRules[0]);
      expect(router.navigate).toHaveBeenCalledWith(['/history'], { queryParams: { entityType: 'Rule', entityId: 'rule-1' } });
    });
  });
});
