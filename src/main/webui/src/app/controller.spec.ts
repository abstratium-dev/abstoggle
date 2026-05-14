import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Controller } from './controller';
import { ModelService, Stage } from './model.service';

describe('Controller', () => {
  let controller: Controller;
  let modelService: ModelService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    controller = TestBed.inject(Controller);
    modelService = TestBed.inject(ModelService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(controller).toBeTruthy();
  });

  describe('loadStages', () => {
    it('should load stages and update model service', () => {
      const mockStages: Stage[] = [
        { id: '1', name: 'Development', displayOrder: 1 },
        { id: '2', name: 'Production', displayOrder: 2 }
      ];

      controller.loadStages();

      const req = httpMock.expectOne('/api/stages');
      expect(req.request.method).toBe('GET');
      req.flush(mockStages);

      expect(modelService.stages$()).toEqual(mockStages);
      expect(modelService.stagesLoading$()).toBe(false);
      expect(modelService.stagesError$()).toBeNull();
    });

    it('should set loading state before request', () => {
      controller.loadStages();

      expect(modelService.stagesLoading$()).toBe(true);
      expect(modelService.stagesError$()).toBeNull();

      const req = httpMock.expectOne('/api/stages');
      req.flush([]);
    });

    it('should handle empty stages list', () => {
      controller.loadStages();

      const req = httpMock.expectOne('/api/stages');
      req.flush([]);

      expect(modelService.stages$()).toEqual([]);
      expect(modelService.stagesLoading$()).toBe(false);
    });

    it('should handle error response', () => {
      controller.loadStages();

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      expect(modelService.stages$()).toEqual([]);
      expect(modelService.stagesLoading$()).toBe(false);
      expect(modelService.stagesError$()).toBe('Failed to load stages');
    });

    it('should handle network error', () => {
      controller.loadStages();

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'));

      expect(modelService.stagesError$()).toBe('Failed to load stages');
    });
  });

  describe('createStage', () => {
    it('should create stage and reload list', async () => {
      const newStage: Stage = { id: '123', name: 'Staging', displayOrder: 1 };
      const allStages: Stage[] = [newStage];

      const createPromise = controller.createStage('Staging', 'Staging environment', 1, undefined);

      const createReq = httpMock.expectOne('/api/stages');
      expect(createReq.request.method).toBe('POST');
      expect(createReq.request.body).toEqual({
        name: 'Staging',
        description: 'Staging environment',
        displayOrder: 1,
        parentStageName: undefined
      });
      createReq.flush(newStage);

      const result = await createPromise;
      expect(result).toEqual(newStage);

      // Verify reload was triggered
      const loadReq = httpMock.expectOne('/api/stages');
      expect(loadReq.request.method).toBe('GET');
      loadReq.flush(allStages);

      expect(modelService.stages$()).toEqual(allStages);
    });

    it('should create stage with parent', async () => {
      const newStage: Stage = { id: '456', name: 'QA', displayOrder: 2, parentStageName: 'Staging' };

      const createPromise = controller.createStage('QA', 'QA environment', 2, 'Staging');

      const createReq = httpMock.expectOne(req => req.method === 'POST' && req.url === '/api/stages');
      expect(createReq.request.body).toEqual({
        name: 'QA',
        description: 'QA environment',
        displayOrder: 2,
        parentStageName: 'Staging'
      });
      createReq.flush(newStage);

      const result = await createPromise;
      expect(result).toEqual(newStage);

      // createStage calls loadStages() after success — flush the resulting GET
      const loadReq = httpMock.expectOne(req => req.method === 'GET' && req.url === '/api/stages');
      loadReq.flush([newStage]);
    });

    it('should throw error on failed creation', async () => {
      const createPromise = controller.createStage('Staging', '', 1, undefined);

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'), { status: 400, statusText: 'Bad Request' });

      await expectAsync(createPromise).toBeRejected();
    });

    it('should handle server error during creation', async () => {
      const createPromise = controller.createStage('Staging', '', 1, undefined);

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      await expectAsync(createPromise).toBeRejected();
    });
  });

  describe('updateStage', () => {
    it('should update stage and reload list', async () => {
      const updatedStage: Stage = { id: '123', name: 'Updated', displayOrder: 3 };
      const allStages: Stage[] = [updatedStage];

      const updatePromise = controller.updateStage('Staging', 'Updated', 'Updated description', 3, undefined);

      const updateReq = httpMock.expectOne('/api/stages/Staging');
      expect(updateReq.request.method).toBe('PUT');
      expect(updateReq.request.body).toEqual({
        name: 'Updated',
        description: 'Updated description',
        displayOrder: 3,
        parentStageName: undefined
      });
      updateReq.flush(updatedStage);

      const result = await updatePromise;
      expect(result).toEqual(updatedStage);

      // Verify reload was triggered
      const loadReq = httpMock.expectOne('/api/stages');
      expect(loadReq.request.method).toBe('GET');
      loadReq.flush(allStages);

      expect(modelService.stages$()).toEqual(allStages);
    });

    it('should throw error on failed update', async () => {
      const updatePromise = controller.updateStage('Staging', 'Updated', '', 1, undefined);

      const req = httpMock.expectOne('/api/stages/Staging');
      req.error(new ProgressEvent('error'), { status: 404, statusText: 'Not Found' });

      await expectAsync(updatePromise).toBeRejected();
    });

    it('should handle validation error during update', async () => {
      const updatePromise = controller.updateStage('Staging', 'Updated', '', 1, undefined);

      const req = httpMock.expectOne('/api/stages/Staging');
      req.error(new ProgressEvent('error'), { status: 400, statusText: 'Bad Request' });

      await expectAsync(updatePromise).toBeRejected();
    });
  });

  describe('deleteStage', () => {
    it('should delete stage and reload list', async () => {
      const stageName = 'Staging';
      const remainingStages: Stage[] = [{ id: '456', name: 'Production', displayOrder: 2 }];

      const deletePromise = controller.deleteStage(stageName);

      const deleteReq = httpMock.expectOne(`/api/stages/${stageName}`);
      expect(deleteReq.request.method).toBe('DELETE');
      deleteReq.flush(null);

      await deletePromise;

      // Verify reload was triggered
      const loadReq = httpMock.expectOne('/api/stages');
      expect(loadReq.request.method).toBe('GET');
      loadReq.flush(remainingStages);

      expect(modelService.stages$()).toEqual(remainingStages);
    });

    it('should throw error on failed deletion', async () => {
      const stageName = 'Staging';
      const deletePromise = controller.deleteStage(stageName);

      const req = httpMock.expectOne(`/api/stages/${stageName}`);
      req.error(new ProgressEvent('error'), { status: 404, statusText: 'Not Found' });

      await expectAsync(deletePromise).toBeRejected();
    });

    it('should handle permission error during deletion', async () => {
      const stageName = 'Staging';
      const deletePromise = controller.deleteStage(stageName);

      const req = httpMock.expectOne(`/api/stages/${stageName}`);
      req.error(new ProgressEvent('error'), { status: 403, statusText: 'Forbidden' });

      await expectAsync(deletePromise).toBeRejected();
    });

    it('should handle server error during deletion', async () => {
      const stageName = 'Staging';
      const deletePromise = controller.deleteStage(stageName);

      const req = httpMock.expectOne(`/api/stages/${stageName}`);
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      await expectAsync(deletePromise).toBeRejected();
    });
  });

  describe('Error Handling', () => {
    it('should log errors to console', () => {
      spyOn(console, 'error');

      controller.loadStages();

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'));

      expect(console.error).toHaveBeenCalledWith('Error loading stages:', jasmine.any(Object));
    });

    it('should log creation errors to console', async () => {
      spyOn(console, 'error');

      const createPromise = controller.createStage('Staging', '', 1, undefined);

      const req = httpMock.expectOne('/api/stages');
      req.error(new ProgressEvent('error'));

      try {
        await createPromise;
      } catch (e) {
        // Expected
      }

      expect(console.error).toHaveBeenCalledWith('Error creating stage:', jasmine.any(Object));
    });
  });

  describe('Integration', () => {
    it('should handle multiple operations in sequence', async () => {
      // Load stages
      controller.loadStages();
      const loadReq1 = httpMock.expectOne('/api/stages');
      loadReq1.flush([{ id: '1', name: 'Dev', displayOrder: 1}]);

      // Create stage
      const createPromise = controller.createStage('Staging', '', 2, undefined);
      const createReq = httpMock.expectOne('/api/stages');
      createReq.flush({ id: '2', name: 'Staging', displayOrder: 2 });
      await createPromise;
      const loadReq2 = httpMock.expectOne('/api/stages');
      loadReq2.flush([
        { id: '1', name: 'Dev', displayOrder: 1 },
        { id: '2', name: 'Staging', displayOrder: 2 }
      ]);

      // Delete stage
      const deletePromise = controller.deleteStage('Dev');
      const deleteReq = httpMock.expectOne('/api/stages/Dev');
      deleteReq.flush(null);
      await deletePromise;
      const loadReq3 = httpMock.expectOne('/api/stages');
      loadReq3.flush([{ id: '2', name: 'Staging', displayOrder: 2 }]);

      expect(modelService.stages$()).toEqual([{ id: '2', name: 'Staging', displayOrder: 2 }]);
    });
  });
});
