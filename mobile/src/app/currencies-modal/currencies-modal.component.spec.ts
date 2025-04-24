import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { CurrenciesModalComponent } from './currencies-modal.component';

describe('CurrenciesModalComponent', () => {
  let component: CurrenciesModalComponent;
  let fixture: ComponentFixture<CurrenciesModalComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CurrenciesModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CurrenciesModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
