import { TestBed } from '@angular/core/testing';
import { ModelService, Stage } from './model.service';

describe('ModelService', () => {
  let service: ModelService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ModelService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should have empty stages initially', () => {
      expect(service.stages$()).toEqual([]);
    });

    it('should not be loading initially', () => {
      expect(service.stagesLoading$()).toBe(false);
    });

    it('should have no error initially', () => {
      expect(service.stagesError$()).toBeNull();
    });
  });

  describe('Stage Management', () => {
    it('should set stages', () => {
      const stages: Stage[] = [
        { id: '1', name: 'Development', displayOrder: 1 },
        { id: '2', name: 'Production', displayOrder: 2 }
      ];
      service.setStages(stages);
      expect(service.stages$()).toEqual(stages);
    });

    it('should update stages', () => {
      const stages1: Stage[] = [{ id: '1', name: 'Dev', displayOrder: 1 }];
      const stages2: Stage[] = [
        { id: '2', name: 'Staging', displayOrder: 2 },
        { id: '3', name: 'Prod', displayOrder: 3 }
      ];

      service.setStages(stages1);
      expect(service.stages$()).toEqual(stages1);

      service.setStages(stages2);
      expect(service.stages$()).toEqual(stages2);
    });

    it('should handle empty stages list', () => {
      const stages: Stage[] = [{ id: '1', name: 'Dev', displayOrder: 1 }];
      service.setStages(stages);
      service.setStages([]);
      expect(service.stages$()).toEqual([]);
    });

    it('should handle large stages list', () => {
      const stages: Stage[] = Array.from({ length: 100 }, (_, i) => ({
        id: `${i}`,
        name: `Stage ${i}`,
        displayOrder: i,
      }));
      service.setStages(stages);
      expect(service.stages$()).toEqual(stages);
      expect(service.stages$().length).toBe(100);
    });
  });

  describe('Loading State Management', () => {
    it('should set loading state', () => {
      service.setStagesLoading(true);
      expect(service.stagesLoading$()).toBe(true);
    });

    it('should update loading state', () => {
      service.setStagesLoading(true);
      expect(service.stagesLoading$()).toBe(true);

      service.setStagesLoading(false);
      expect(service.stagesLoading$()).toBe(false);
    });

    it('should toggle loading state multiple times', () => {
      service.setStagesLoading(true);
      service.setStagesLoading(false);
      service.setStagesLoading(true);
      expect(service.stagesLoading$()).toBe(true);
    });
  });

  describe('Error State Management', () => {
    it('should set error', () => {
      service.setStagesError('Failed to load stages');
      expect(service.stagesError$()).toBe('Failed to load stages');
    });

    it('should update error', () => {
      service.setStagesError('Error 1');
      expect(service.stagesError$()).toBe('Error 1');

      service.setStagesError('Error 2');
      expect(service.stagesError$()).toBe('Error 2');
    });

    it('should clear error', () => {
      service.setStagesError('Some error');
      service.setStagesError(null);
      expect(service.stagesError$()).toBeNull();
    });

    it('should handle empty string error', () => {
      service.setStagesError('');
      expect(service.stagesError$()).toBe('');
    });
  });

  describe('Combined State Management', () => {
    it('should manage all states independently', () => {
      const stages: Stage[] = [{ id: '1', name: 'Dev', displayOrder: 1 }];
      service.setStages(stages);
      service.setStagesLoading(true);
      service.setStagesError('Some error');

      expect(service.stages$()).toEqual(stages);
      expect(service.stagesLoading$()).toBe(true);
      expect(service.stagesError$()).toBe('Some error');
    });

    it('should reset all states', () => {
      service.setStages([{ id: '1', name: 'Dev', displayOrder: 1 }]);
      service.setStagesLoading(true);
      service.setStagesError('Error');

      service.setStages([]);
      service.setStagesLoading(false);
      service.setStagesError(null);

      expect(service.stages$()).toEqual([]);
      expect(service.stagesLoading$()).toBe(false);
      expect(service.stagesError$()).toBeNull();
    });
  });

  describe('Signal Reactivity', () => {
    it('should emit signal updates for stages', () => {
      const stages1: Stage[] = [{ id: '1', name: 'Dev', displayOrder: 1 }];
      const stages2: Stage[] = [{ id: '2', name: 'Prod', displayOrder: 2 }];

      service.setStages(stages1);
      expect(service.stages$()).toEqual(stages1);

      service.setStages(stages2);
      expect(service.stages$()).toEqual(stages2);
    });

    it('should emit signal updates for loading', () => {
      service.setStagesLoading(true);
      expect(service.stagesLoading$()).toBe(true);

      service.setStagesLoading(false);
      expect(service.stagesLoading$()).toBe(false);
    });

    it('should emit signal updates for error', () => {
      service.setStagesError('Error 1');
      expect(service.stagesError$()).toBe('Error 1');

      service.setStagesError('Error 2');
      expect(service.stagesError$()).toBe('Error 2');
    });
  });

  describe('Service Singleton', () => {
    it('should be a singleton across injections', () => {
      const service2 = TestBed.inject(ModelService);
      service.setStages([{ id: '1', name: 'Dev', displayOrder: 1 }]);
      expect(service2.stages$()).toEqual([{ id: '1', name: 'Dev', displayOrder: 1 }]);
    });
  });
});
