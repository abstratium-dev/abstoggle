import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StagesComponent } from './stages.component';
import { Controller } from '../controller';
import { ModelService, Stage } from '../model.service';
import { ToastService } from '../core/toast/toast.service';
import { ConfirmDialogService } from '../core/confirm-dialog/confirm-dialog.service';
import { ChangeNoteDialogService } from '../core/change-note-dialog/change-note-dialog.service';
import { signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

describe('StagesComponent', () => {
  let component: StagesComponent;
  let fixture: ComponentFixture<StagesComponent>;
  let controller: jasmine.SpyObj<Controller>;
  let modelService: jasmine.SpyObj<ModelService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let confirmService: jasmine.SpyObj<ConfirmDialogService>;
  let router: jasmine.SpyObj<Router>;

  // Store signal references for testing
  let stagesSignal: ReturnType<typeof signal<Stage[]>>;
  let loadingSignal: ReturnType<typeof signal<boolean>>;
  let errorSignal: ReturnType<typeof signal<string | null>>;

  const mockStages: Stage[] = [
    { id: '1', name: 'Development', description: 'Dev environment', displayOrder: 1,  },
    { id: '2', name: 'Staging', description: 'Staging environment', displayOrder: 2, parentStageName: 'Development' },
    { id: '3', name: 'Production', description: 'Production environment', displayOrder: 3 }
  ];

  beforeEach(async () => {
    const controllerSpy = jasmine.createSpyObj('Controller', [
      'loadStages', 'createStage', 'updateStage', 'deleteStage'
    ]);

    // Create writable signals for testing
    stagesSignal = signal<Stage[]>([]);
    loadingSignal = signal<boolean>(false);
    errorSignal = signal<string | null>(null);

    const modelServiceSpy = jasmine.createSpyObj('ModelService', [], {
      stages$: stagesSignal,
      stagesLoading$: loadingSignal,
      stagesError$: errorSignal,
      config$: signal({ changeNoteMandatory: false })
    });
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const confirmServiceSpy = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);
    const changeNoteDialogSpy = jasmine.createSpyObj('ChangeNoteDialogService', ['prompt']);
    changeNoteDialogSpy.prompt.and.returnValue(Promise.resolve('Test change note'));
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [StagesComponent],
      providers: [
        { provide: Controller, useValue: controllerSpy },
        { provide: ModelService, useValue: modelServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ConfirmDialogService, useValue: confirmServiceSpy },
        { provide: ChangeNoteDialogService, useValue: changeNoteDialogSpy },
        { provide: ActivatedRoute, useValue: { queryParamMap: of({ get: () => null }) } },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    controller = TestBed.inject(Controller) as jasmine.SpyObj<Controller>;
    modelService = TestBed.inject(ModelService) as jasmine.SpyObj<ModelService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    confirmService = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    fixture = TestBed.createComponent(StagesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load stages on init', () => {
      fixture.detectChanges();
      expect(controller.loadStages).toHaveBeenCalled();
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
      component.stageName = 'Test';
      component.stageDescription = 'Description';
      component.stageDisplayOrder = 5;
      component.formError = 'Some error';

      component.toggleAddForm();

      expect(component.stageName).toBe('');
      expect(component.stageDescription).toBe('');
      expect(component.stageDisplayOrder).toBe(0);
      expect(component.formError).toBeNull();
    });

    it('should start editing a stage', () => {
      const stage: Stage = mockStages[0];

      component.startEdit(stage);

      expect(component.editingStage).toBe(stage);
      expect(component.showAddForm).toBe(true);
      expect(component.stageName).toBe(stage.name);
      expect(component.stageDescription).toBe(stage.description || '');
      expect(component.stageDisplayOrder).toBe(stage.displayOrder);
    });

    it('should cancel editing', () => {
      component.editingStage = mockStages[0];
      component.showAddForm = true;
      component.stageName = 'Test';

      component.cancelEdit();

      expect(component.editingStage).toBeNull();
      expect(component.showAddForm).toBe(false);
      expect(component.stageName).toBe('');
    });

    it('should reset form fields', () => {
      component.stageName = 'Test';
      component.stageDescription = 'Description';
      component.stageDisplayOrder = 5;
      component.stageParentName = 'Parent';
      component.changeNote = 'Test note';
      component.formError = 'Error';

      component.resetForm();

      expect(component.stageName).toBe('');
      expect(component.stageDescription).toBe('');
      expect(component.stageDisplayOrder).toBe(0);
      expect(component.stageParentName).toBe('');
      expect(component.changeNote).toBe('');
      expect(component.formError).toBeNull();
    });
  });

  describe('Create Stage', () => {
    it('should require stage name', async () => {
      component.stageName = '';

      await component.onSubmit();

      expect(component.formError).toBe('Stage name is required');
      expect(controller.createStage).not.toHaveBeenCalled();
    });

    it('should create stage successfully', async () => {
      const newStage: Stage = {
        id: '4',
        name: 'New Stage',
        description: 'New Description',
        displayOrder: 4
      };
      controller.createStage.and.returnValue(Promise.resolve(newStage));

      component.stageName = 'New Stage';
      component.stageDescription = 'New Description';
      component.stageDisplayOrder = 4;

      await component.onSubmit();

      expect(controller.createStage).toHaveBeenCalledWith(
        'New Stage',
        'New Description',
        4,
        undefined,
        ''
      );
      expect(toastService.success).toHaveBeenCalledWith('Stage created successfully');
      expect(component.showAddForm).toBe(false);
      expect(component.formSubmitting).toBe(false);
    });

    it('should create stage with parent', async () => {
      const newStage: Stage = {
        id: '4',
        name: 'Child Stage',
        description: '',
        displayOrder: 2,
        parentStageName: 'Development'
      };
      controller.createStage.and.returnValue(Promise.resolve(newStage));

      component.stageName = 'Child Stage';
      component.stageDescription = '';
      component.stageDisplayOrder = 2;
      component.stageParentName = 'Development';

      await component.onSubmit();

      expect(controller.createStage).toHaveBeenCalledWith(
        'Child Stage',
        '',
        2,
        'Development',
        ''
      );
    });

    it('should handle create error', async () => {
      controller.createStage.and.returnValue(Promise.reject({
        error: { detail: 'Stage already exists' }
      }));

      component.stageName = 'Existing Stage';

      await component.onSubmit();

      expect(controller.createStage).toHaveBeenCalled();
      expect(component.formError).toBe('Stage already exists');
      expect(component.formSubmitting).toBe(false);
    });

    it('should trim whitespace from stage name', async () => {
      controller.createStage.and.returnValue(Promise.resolve(mockStages[0]));

      component.stageName = '  Test Stage  ';
      component.stageDescription = '  Test Description  ';

      await component.onSubmit();

      expect(controller.createStage).toHaveBeenCalledWith(
        'Test Stage',
        'Test Description',
        0,
        undefined,
        ''
      );
    });
  });

  describe('Update Stage', () => {
    it('should update stage successfully', async () => {
      const updatedStage: Stage = {
        ...mockStages[0],
        name: 'Updated Name',
        description: 'Updated Description'
      };
      controller.updateStage.and.returnValue(Promise.resolve(updatedStage));

      component.editingStage = mockStages[0];
      component.stageName = 'Updated Name';
      component.stageDescription = 'Updated Description';
      component.stageDisplayOrder = 1;

      await component.onSubmit();

      expect(controller.updateStage).toHaveBeenCalledWith(
        '1',
        'Updated Name',
        'Updated Description',
        1,
        undefined,
        ''
      );
      expect(toastService.success).toHaveBeenCalledWith('Stage updated successfully');
      expect(component.showAddForm).toBe(false);
      expect(component.editingStage).toBeNull();
    });
  });

  describe('Delete Stage', () => {
    it('should delete stage after confirmation', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(true));
      controller.deleteStage.and.returnValue(Promise.resolve());

      await component.deleteStage(mockStages[0]);

      expect(confirmService.confirm).toHaveBeenCalledWith({
        title: 'Delete Stage',
        message: 'Are you sure you want to delete the stage "Development"? This action cannot be undone.',
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmClass: 'btn-danger'
      });
      expect(controller.deleteStage).toHaveBeenCalledWith('1', jasmine.any(String));
      expect(toastService.success).toHaveBeenCalledWith('Stage deleted successfully');
    });

    it('should not delete if user cancels', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(false));

      await component.deleteStage(mockStages[0]);

      expect(confirmService.confirm).toHaveBeenCalled();
      expect(controller.deleteStage).not.toHaveBeenCalled();
      expect(toastService.success).not.toHaveBeenCalled();
    });

    it('should show error toast on delete failure', async () => {
      confirmService.confirm.and.returnValue(Promise.resolve(true));
      controller.deleteStage.and.returnValue(Promise.reject({
        error: { detail: 'Cannot delete stage with children' }
      }));

      await component.deleteStage(mockStages[0]);

      expect(controller.deleteStage).toHaveBeenCalledWith('1', jasmine.any(String));
      expect(toastService.error).toHaveBeenCalledWith('Cannot delete stage with children');
    });
  });

  describe('Helper Methods', () => {
    it('should return parent stage name or dash', () => {
      expect(component.getParentStageName(mockStages[0])).toBe('-');
      expect(component.getParentStageName(mockStages[1])).toBe('Development');
    });

    it('should get available parent stages excluding current stage', () => {
      stagesSignal.set(mockStages);

      const available = component.getAvailableParentStages(mockStages[0]);

      expect(available.length).toBe(1);
      expect(available.find(s => s.name === 'Development')).toBeUndefined();
      expect(available.find(s => s.name === 'Staging')).toBeUndefined();
    });

    it('should include all stages when no current stage', () => {
      stagesSignal.set(mockStages);

      const available = component.getAvailableParentStages(undefined);

      expect(available.length).toBe(3);
    });
  });

  describe('Signal Integration', () => {
    it('should use stages signal from model service', () => {
      expect(component.stages).toBe(modelService.stages$);
    });

    it('should use loading signal from model service', () => {
      expect(component.loading).toBe(modelService.stagesLoading$);
    });

    it('should use error signal from model service', () => {
      expect(component.error).toBe(modelService.stagesError$);
    });
  });

  describe('Retry', () => {
    it('should call controller loadStages on retry', () => {
      component.onRetry();
      expect(controller.loadStages).toHaveBeenCalled();
    });
  });

  describe('Navigation', () => {
    it('goToToggles should navigate to toggles filtered by stage name', () => {
      component.goToToggles(mockStages[0]);
      expect(router.navigate).toHaveBeenCalledWith(['/toggles'], { queryParams: { filterStage: 'Development' } });
    });

    it('goToHistory should navigate to history with Stage entity params', () => {
      component.goToHistory(mockStages[0]);
      expect(router.navigate).toHaveBeenCalledWith(['/history'], { queryParams: { entityType: 'Stage', entityId: '1' } });
    });
  });
});
