import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InfoButtonComponent } from './info-button.component';

describe('InfoButtonComponent', () => {
  let component: InfoButtonComponent;
  let fixture: ComponentFixture<InfoButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InfoButtonComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(InfoButtonComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('tooltipText', 'Test tooltip');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render tooltip text', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Test tooltip');
  });

  it('should have aria-label', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const button = compiled.querySelector('.info-button');
    expect(button?.getAttribute('aria-label')).toBe('Test tooltip');
  });
});
