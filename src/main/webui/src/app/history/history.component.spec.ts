import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HistoryComponent } from './history.component';
import { Controller } from '../controller';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { ParamMap, convertToParamMap } from '@angular/router';
import { HistoryEntry, HistoryChange, EntityRevision } from '../model.service';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let controller: jasmine.SpyObj<Controller>;
  let router: jasmine.SpyObj<Router>;
  let queryParamSubject: Subject<ParamMap>;

  const mockEntries: HistoryEntry[] = [
    { rev: 10, timestamp: 1700000000000, username: 'alice', changeNote: 'First change', correlationId: 'c1' },
    { rev: 9,  timestamp: 1699999999000, username: 'bob',   changeNote: 'Second change', correlationId: 'c2' }
  ];

  const mockEntityHistory: EntityRevision[] = [
    { rev: 5, timestamp: 1700000001000, username: 'alice', changeNote: 'Created', revtype: 0, data: 'name=dev' },
    { rev: 6, timestamp: 1700000002000, username: 'bob',   changeNote: 'Updated', revtype: 1, data: 'name=development' }
  ];

  beforeEach(async () => {
    queryParamSubject = new Subject<ParamMap>();

    const controllerSpy = jasmine.createSpyObj('Controller', [
      'loadHistory', 'getRevisionDetails', 'getEntityHistory'
    ]);
    controllerSpy.loadHistory.and.returnValue(Promise.resolve(mockEntries));
    controllerSpy.getRevisionDetails.and.returnValue(Promise.resolve([]));
    controllerSpy.getEntityHistory.and.returnValue(Promise.resolve(mockEntityHistory));

    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [HistoryComponent],
      providers: [
        { provide: Controller, useValue: controllerSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { queryParamMap: queryParamSubject.asObservable() } }
      ]
    }).compileComponents();

    controller = TestBed.inject(Controller) as jasmine.SpyObj<Controller>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Normal history mode', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      queryParamSubject.next(convertToParamMap({}));
      tick();
    }));

    it('should load history on init when no entity params present', () => {
      expect(controller.loadHistory).toHaveBeenCalled();
      expect(component.entityMode).toBeFalse();
    });

    it('should populate entries after loadHistory', () => {
      expect(component.entries).toEqual(mockEntries);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    });

    it('should set error on loadHistory failure', fakeAsync(() => {
      controller.loadHistory.and.returnValue(Promise.reject(new Error('network error')));
      component.loadHistory();
      tick();
      expect(component.error).toBe('Failed to load history');
      expect(component.loading).toBeFalse();
    }));

    it('onSearch should reset offset and reload history', fakeAsync(() => {
      component.offset = 50;
      component.onSearch();
      tick();
      expect(component.offset).toBe(0);
      expect(controller.loadHistory).toHaveBeenCalled();
    }));

    it('onNextPage should increment offset when full page returned', fakeAsync(() => {
      component.entries = new Array(50).fill(mockEntries[0]);
      component.limit = 50;
      component.offset = 0;
      component.onNextPage();
      tick();
      expect(component.offset).toBe(50);
      expect(controller.loadHistory).toHaveBeenCalled();
    }));

    it('onNextPage should not increment offset when partial page returned', fakeAsync(() => {
      component.entries = mockEntries;
      component.limit = 50;
      component.offset = 0;
      component.onNextPage();
      tick();
      expect(component.offset).toBe(0);
    }));

    it('onPrevPage should decrement offset', fakeAsync(() => {
      component.offset = 50;
      component.limit = 50;
      component.onPrevPage();
      tick();
      expect(component.offset).toBe(0);
      expect(controller.loadHistory).toHaveBeenCalled();
    }));

    it('onPrevPage should not go below 0', fakeAsync(() => {
      component.offset = 0;
      component.onPrevPage();
      tick();
      expect(component.offset).toBe(0);
    }));

    it('selectEntry should load detail changes for a revision', fakeAsync(() => {
      const mockChanges: HistoryChange[] = [
        { table: 'Stage', entityId: 'abc', revtype: 1, data: 'name=dev' }
      ];
      controller.getRevisionDetails.and.returnValue(Promise.resolve(mockChanges));
      component.selectEntry(mockEntries[0]);
      tick();
      expect(component.selectedEntry).toEqual(mockEntries[0]);
      expect(component.detailChanges).toEqual(mockChanges);
    }));

    it('selectEntry same entry should deselect', fakeAsync(() => {
      component.selectedEntry = mockEntries[0];
      component.selectEntry(mockEntries[0]);
      tick();
      expect(component.selectedEntry).toBeNull();
    }));

    it('toggleEntityHistory should load entity history for a key', fakeAsync(() => {
      component.toggleEntityHistory('Stage', 'abc-123');
      tick();
      expect(controller.getEntityHistory).toHaveBeenCalledWith('Stage', 'abc-123');
      expect(component.entityHistory).toEqual(mockEntityHistory);
      expect(component.selectedEntityKey).toBe('Stage:abc-123');
    }));

    it('toggleEntityHistory same key should deselect', fakeAsync(() => {
      component.selectedEntityKey = 'Stage:abc-123';
      component.toggleEntityHistory('Stage', 'abc-123');
      tick();
      expect(component.selectedEntityKey).toBeNull();
      expect(component.entityHistory).toEqual([]);
    }));
  });

  describe('Entity mode', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      queryParamSubject.next(convertToParamMap({ entityType: 'Stage', entityId: 'abc-123' }));
      tick();
    }));

    it('should enter entity mode when entityType and entityId query params are present', () => {
      expect(component.entityMode).toBeTrue();
      expect(component.entityModeType).toBe('Stage');
      expect(component.entityModeId).toBe('abc-123');
    });

    it('should call getEntityHistory with correct params', () => {
      expect(controller.getEntityHistory).toHaveBeenCalledWith('Stage', 'abc-123');
    });

    it('should populate entityHistory after loading', () => {
      expect(component.entityHistory).toEqual(mockEntityHistory);
      expect(component.entityHistoryLoading).toBeFalse();
      expect(component.entityHistoryError).toBeNull();
    });

    it('should set entityHistoryError on failure', fakeAsync(() => {
      controller.getEntityHistory.and.returnValue(Promise.reject(new Error('fail')));
      component.loadEntityModeHistory();
      tick();
      expect(component.entityHistoryError).toBe('Failed to load entity history');
      expect(component.entityHistoryLoading).toBeFalse();
    }));

    it('should NOT call loadHistory when in entity mode', () => {
      expect(controller.loadHistory).not.toHaveBeenCalled();
    });

    it('backToHistory should navigate to /history', () => {
      component.backToHistory();
      expect(router.navigate).toHaveBeenCalledWith(['/history']);
    });
  });

  describe('Utility methods', () => {
    it('formatTimestamp should return locale string', () => {
      const ts = new Date('2024-01-15T10:30:00Z').getTime();
      const result = component.formatTimestamp(ts);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('revtypeLabel should return ADD for 0', () => {
      expect(component.revtypeLabel(0)).toBe('ADD');
    });

    it('revtypeLabel should return MOD for 1', () => {
      expect(component.revtypeLabel(1)).toBe('MOD');
    });

    it('revtypeLabel should return DEL for 2', () => {
      expect(component.revtypeLabel(2)).toBe('DEL');
    });

    it('revtypeLabel should return string for unknown', () => {
      expect(component.revtypeLabel(99)).toBe('99');
    });

    it('revtypeCssClass should return badge-add for 0', () => {
      expect(component.revtypeCssClass(0)).toBe('badge-add');
    });

    it('revtypeCssClass should return badge-mod for 1', () => {
      expect(component.revtypeCssClass(1)).toBe('badge-mod');
    });

    it('revtypeCssClass should return badge-del for 2', () => {
      expect(component.revtypeCssClass(2)).toBe('badge-del');
    });

    it('revtypeCssClass should return empty string for unknown', () => {
      expect(component.revtypeCssClass(99)).toBe('');
    });
  });
});
